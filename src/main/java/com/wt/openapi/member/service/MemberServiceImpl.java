package com.wt.openapi.member.service;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wt.openapi.common.model.RequestCustomHeaderInfo;
import com.wt.openapi.common.model.ResultCode;
import com.wt.openapi.common.model.ResultMaster;
import com.wt.openapi.common.session.SessionCheckInfoByDB;
import com.wt.openapi.member.dao.MemberDao;
import com.wt.openapi.member.model.info.EmailAuthInfo;
import com.wt.openapi.member.model.info.JoinMemberInfo;
import com.wt.openapi.member.model.info.LoginMemberInfo;
import com.wt.openapi.member.model.info.ModifyPwdInfo;
import com.wt.openapi.member.model.vo.MemberInfo;
import com.wt.openapi.member.model.vo.MemberPwdInfo;
import com.wt.openapi.utils.CodeMessage;
import com.wt.openapi.utils.EncryptionUtils;
import com.wt.openapi.utils.SendEmailUtils;
import com.wt.openapi.utils.StringUtils;


@Service
public class MemberServiceImpl implements MemberService{
	
	Log logger = LogFactory.getLog(getClass());
	
	EncryptionUtils encryptionUtils = new EncryptionUtils();
	SendEmailUtils sendEmailUtils = new SendEmailUtils();
	
	@Autowired
 	private MemberDao memberDao;
	
	@Autowired
	SessionCheckInfoByDB sessionCheckInfo;
 
    /*
	 * 회원가입
	 * 가입 시 memId(이메일)로 이메일 인증 메일 발송
	 * 비밀번호 생성규칙 : 임의의 saltKey(난수를 생성하여 md5로 16진수 암호화) + 비밀번호(영문+숫자+특수문자 합하여 8자리 이상) 두개를 합하여 sha256으로 암호화
	 * 이메일 인증키 생성규칙 : emailAuthKey(기본 스트링) + 회원ID + 현재 시간 sha256암호화
	 * 만료시간은 1시간
	 */
	@Override
	public ResultMaster joinMember(JoinMemberInfo joinMemberInfo) {
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		try {
			MemberInfo memberInfo = new MemberInfo();
			// 이메일 인증키 생성
			String emailAuthKey = encryptionUtils.getSHA256Digest("emailAuthKey"+joinMemberInfo.getUserId()+System.currentTimeMillis());
			
			logger.info("MemberImple[joinMemberInfo] : " + joinMemberInfo.toString());
			memberInfo.setMemStatus("1");
			memberInfo.setMemType("1");
			memberInfo.setMemSex(joinMemberInfo.getSex());
			memberInfo.setMemName(joinMemberInfo.getName());
			memberInfo.setMemPhone(joinMemberInfo.getPhone());
			memberInfo.setMemNickName(joinMemberInfo.getNickName());
			memberInfo.setMemEmail(joinMemberInfo.getUserId());
			memberInfo.setMemEmailStatus("0");
			memberInfo.setMemEmailKey(emailAuthKey);
			
			if (StringUtils.isNotNVL(joinMemberInfo.getBirth_year())  &&
					StringUtils.isNotNVL(joinMemberInfo.getBirth_year())  &&
					StringUtils.isNotNVL(joinMemberInfo.getBirth_year()) )
				memberInfo.setMemBirth(joinMemberInfo.getBirth_year() + joinMemberInfo.getBirth_month() + joinMemberInfo.getBirth_day());
			else {
				memberInfo.setMemBirth(joinMemberInfo.getBirth());
			}
			
			memberInfo.setMemWeight(joinMemberInfo.getWeight());
			memberInfo.setMemHeight(joinMemberInfo.getHeight());
			
			logger.info("memberInfo : " + memberInfo.toString());
			// 회원정보 생성
			if(memberDao.joinMember(memberInfo) > 0) {
				// 비밀번호 생성
				HashMap<String, String> encryption = encryptionUtils.encryption(joinMemberInfo.getPassword());
				
				MemberPwdInfo memberPwdInfo = new MemberPwdInfo();
				memberPwdInfo.setMemNo(memberInfo.getMemNo());
				memberPwdInfo.setMemSalt(encryption.get("saltKey"));
				memberPwdInfo.setMemPassword(encryption.get("encryptionStr"));

				if(memberDao.insertPwd(memberPwdInfo) > 0) {
					// 이메일 인증 메일 발송
					sendEmailUtils.SendEmailAuth(emailAuthKey, joinMemberInfo.getUserId(), memberInfo.getMemNo());
					CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
				}
			}
		} catch (Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_MEMBER_JOIN_FAIL),e);
			CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_MEMBER_JOIN_FAIL);
		}
		
		rm.setResult(resultCode);
		
    	return rm;
	}

	/*
	 * 로그인
	 * 인증 완료된 회원 로그인
	 * 세션 정보 생성
	 * 비밀번호 검증 절차 : 회원의 고유 saltKey조회 + 전달받은 비밀번호를 sha256으로 암호화하여 비밀번호 비교
	 */
	@Override
	public ResultMaster loginMember(LoginMemberInfo loginMemberInfo, RequestCustomHeaderInfo requestNotLoginHeaderInfo, HttpServletResponse response) {
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		try {
			HashMap<String, Object> param = new HashMap<String, Object>();
			param.put("memEmail", loginMemberInfo.getUserId());
			MemberInfo resultMember = memberDao.selectMember(param);
			
			if(resultMember != null) {
				requestNotLoginHeaderInfo.setMemNo(resultMember.getMemNo());
				
				// 미인증 회원의 경우 세션정보에 memNo만 전달
				if(resultMember.getMemStatus().equals(CodeMessage.USER_STATUS_NOT_AUTH)) {
					CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_MEMBER_NOT_AUTH);
				} else {
					// 비밀번호 검증
					MemberPwdInfo memberPwdInfo = memberDao.selectPwd(resultMember.getMemNo());
					String resultPwd = encryptionUtils.getSHA256Digest(memberPwdInfo.getMemSalt()+loginMemberInfo.getPassword());
					
					if(memberPwdInfo.getMemPassword().equals(resultPwd)) {						
						// 세션 정보 생성
						requestNotLoginHeaderInfo = sessionCheckInfo.makeSessionId(resultMember, requestNotLoginHeaderInfo);
						CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
						
						response.setHeader("session-id", requestNotLoginHeaderInfo.getSessionid());
						response.setHeader("user-id", requestNotLoginHeaderInfo.getUserId());
						response.setHeader("poc-id", requestNotLoginHeaderInfo.getPocId());
						
						rm.setBody(resultMember);
					} else {
						CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_MEMBER_PASSWORD_NOT_EQUAL);
					}
				}
			} else {
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_MEMBER_SEARCH_FAIL);
			}
		} catch (Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_MEMBER_LOGIN_FAIL),e);
			CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_MEMBER_LOGIN_FAIL);
		}
		
		rm.setResult(resultCode);
		
    	return rm;
	}

	@Override
	public ResultMaster logoutMember(RequestCustomHeaderInfo requestLoginHeaderInfo) {
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		try {
			// 세션 삭제
			if(sessionCheckInfo.deleteSessionId(requestLoginHeaderInfo.getSessionid()) > 0) {
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
			} else {
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_SESSION_DELETE_FAIL);
			}
		} catch (Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_SESSION_DELETE_FAIL),e);
			CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_SESSION_DELETE_FAIL);
		}
		
		rm.setResult(resultCode);
		
    	return rm;
	}

	@Override
	public ResultMaster emailAuth(EmailAuthInfo emailAuthInfo) {
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		try {
			// 회원정보 업데이트
			if(memberDao.updateEmailAuth(emailAuthInfo) > 0) {
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
			} else {
				CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_EMAIL_AUTH_FAIL);
			}
		} catch (Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_EMAIL_AUTH_FAIL),e);
			CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_EMAIL_AUTH_FAIL);
		}

		rm.setResult(resultCode);
		
    	return rm;
	}
	
	@Override
	public ResultMaster checkId(Map<String, Object> param)
	{
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		String userId = StringUtils.nvlStr(param.get("userId"));
		if("".equals(userId)) {
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_NOT_PARAMETER);
			rm.setResult(resultCode);
			return rm;
		}
		
		try	{
			param.put("userId", userId);
		} catch(Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_OVERLAP_ID), e);
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OVERLAP_ID);
			rm.setResult(resultCode);
			return rm;
		}
		
		try {
			int result = memberDao.checkId(userId);
			
			if(result == 0)
			{
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
			}
			else
			{
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OVERLAP_ID);
				rm.setResult(resultCode);
				return rm;
			}
		} catch(Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_SERVICE_ERROR), e);
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_SERVICE_ERROR);
			rm.setResult(resultCode);
			return rm;
		}
		
		rm.setResult(resultCode);
		return rm;
	}
	
	/*
	 * 비밀번호 변경
	 */
	@Override
	public ResultMaster modifyPwd(ModifyPwdInfo modifyPwdInfo, String memNo) {
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		try {
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_MEMBER_PASSWORD_MODITY_FAIL);
			
			// 현재 비밀번호와 바꿀 비밀번호 검증
			if(!modifyPwdInfo.getChangePw().equals(modifyPwdInfo.getMemPw())) {
				MemberPwdInfo memberPwdInfo = memberDao.selectPwd(memNo);
				String resultPwd = encryptionUtils.getSHA256Digest(memberPwdInfo.getMemSalt()+modifyPwdInfo.getMemPw());
				
				// 현재 비밀번호 검증
				if(memberPwdInfo.getMemPassword().equals(resultPwd)) {
					
					// 신규 비밀번호 발급
					HashMap<String, String> encryption = encryptionUtils.encryption(modifyPwdInfo.getChangePw());
					
					MemberPwdInfo newPwdInfo = new MemberPwdInfo();
					newPwdInfo.setMemNo(memNo);
					newPwdInfo.setMemSalt(encryption.get("saltKey"));
					newPwdInfo.setMemPassword(encryption.get("encryptionStr"));
					
					// 비밀번호 업데이트
					if(memberDao.modifyPwd(newPwdInfo) > 0) {
						CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
					}
				}
			}
		} catch (Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_MEMBER_PASSWORD_MODITY_FAIL),e);
			CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_MEMBER_PASSWORD_MODITY_FAIL);
		}
		
		rm.setResult(resultCode);
		
    	return rm;
	}

	@Override
	public ResultMaster getTempPwd(Map<String, Object> param) {
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		try {
			HashMap<String, Object> search = new HashMap<String, Object>();
			search.put("memEmail", param.get("userId"));
			MemberInfo resultMember = memberDao.selectMember(search);
			
			if(resultMember != null) {
				String tempPassword = encryptionUtils.getTempPassword();
				
				// 비밀번호 생성
				HashMap<String, String> encryption = encryptionUtils.encryption(tempPassword);
				
				MemberPwdInfo memberPwdInfo = new MemberPwdInfo();
				memberPwdInfo.setMemNo(resultMember.getMemNo());
				memberPwdInfo.setMemSalt(encryption.get("saltKey"));
				memberPwdInfo.setMemPassword(encryption.get("encryptionStr"));

				if(memberDao.modifyPwd(memberPwdInfo) > 0) {
					// 이메일 인증 메일 발송
					sendEmailUtils.SendModifyPwd((String)param.get("userId"), tempPassword);
					CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
				}
			} else {
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_MEMBER_SEARCH_FAIL);
			}
		} catch (Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_MEMBER_TEMP_PASSWORD_FAIL),e);
			CodeMessage.setResultCode(resultCode,CodeMessage.RESPONSE_CODE_MEMBER_TEMP_PASSWORD_FAIL);
		}
		
		rm.setResult(resultCode);
		
    	return rm;
	}
	

	@Override
	public ResultMaster checkNickName(Map<String, Object> param)
	{
		ResultMaster rm = new ResultMaster();
		ResultCode resultCode = new ResultCode();
		
		String userNickName = StringUtils.nvlStr(param.get("userNickName"));
		if("".equals(userNickName)) {
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_NOT_PARAMETER);
			rm.setResult(resultCode);
			return rm;
		}
		
		try	{
			param.put("userNickName", userNickName);
		} catch(Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_OVERLAP_NICKNAME), e);
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OVERLAP_NICKNAME);
			rm.setResult(resultCode);
			return rm;
		}
		
		try {
			int result = memberDao.checkNickName(userNickName);
			
			if(result == 0)
			{
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OK);
			}
			else
			{
				CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_OVERLAP_NICKNAME);
				rm.setResult(resultCode);
				return rm;
			}
		} catch(Exception e) {
			logger.error(CodeMessage.getResultMsg(CodeMessage.RESPONSE_CODE_SERVICE_ERROR), e);
			CodeMessage.setResultCode(resultCode, CodeMessage.RESPONSE_CODE_SERVICE_ERROR);
			rm.setResult(resultCode);
			return rm;
		}
		
		rm.setResult(resultCode);
		return rm;
	}
}