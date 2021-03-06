package com.wt.openapi.member.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.wt.openapi.common.configuration.ConfigProperties;
import com.wt.openapi.common.controller.GeneralController;
import com.wt.openapi.common.model.RequestCustomHeaderInfo;
import com.wt.openapi.common.model.ResultCode;
import com.wt.openapi.common.model.ResultMaster;
import com.wt.openapi.member.model.info.EmailAuthInfo;
import com.wt.openapi.member.model.info.JoinMemberInfo;
import com.wt.openapi.member.model.info.LoginMemberInfo;
import com.wt.openapi.member.model.info.ModifyPwdInfo;
import com.wt.openapi.member.service.MemberService;
import com.wt.openapi.utils.CodeMessage;
import com.wt.openapi.utils.HttpRequest;
import com.wt.openapi.utils.StringUtils;

@RequestMapping(value = "/openapi/member")
@Controller
public class MemberController extends GeneralController{
	
	@Autowired
	MemberService memberService;
	
	/*
	 * 회원가입
	 */
	@RequestMapping(value = "/joinByClient", method = RequestMethod.POST, produces="application/json")
	@ResponseBody
	public ResultMaster joinMember(HttpServletRequest request, @ModelAttribute @Valid JoinMemberInfo joinMemberInfo, BindingResult bindingResult) {
		ResultMaster rm = new ResultMaster();
		
		// not login haeder validation check
		RequestCustomHeaderInfo requestCustomHeaderInfo = geNotLoginRequestHeaderInfo(request);
		
		if((!requestCustomHeaderInfo.getErrorCode().equals(CodeMessage.RESPONSE_CODE_OK)) || bindingResult.hasErrors()) {
        	ResultCode result = CodeMessage.setResultCode(CodeMessage.RESPONSE_CODE_NOT_PARAMETER);
        	rm.setResult(result);
		} else {
			rm = memberService.joinMember(joinMemberInfo);
		}
		
		return rm;
	}
	
	/*
	 * 로그인
	 */
	@RequestMapping(value = "/login", method = RequestMethod.POST, produces="application/json")
	@ResponseBody
	public ResultMaster loginMember(HttpServletRequest request, HttpServletResponse response, @ModelAttribute @Valid LoginMemberInfo loginMemberInfo, BindingResult bindingResult) {
		ResultMaster rm = new ResultMaster();
		
		// not login haeder validation check
		RequestCustomHeaderInfo requestCustomHeaderInfo = geNotLoginRequestHeaderInfo(request);
		
		if((!requestCustomHeaderInfo.getErrorCode().equals(CodeMessage.RESPONSE_CODE_OK)) || bindingResult.hasErrors()) {
        	ResultCode result = CodeMessage.setResultCode(CodeMessage.RESPONSE_CODE_NOT_PARAMETER);
        	rm.setResult(result);
		} else {
			rm = memberService.loginMember(loginMemberInfo, requestCustomHeaderInfo, response);
		}
		
		return rm;
	}
	
	/*
	 * 로그아웃
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.DELETE, produces="application/json")
	@ResponseBody
	public ResultMaster logoutMember(HttpServletRequest request) {
		ResultMaster rm = new ResultMaster();
		
		// haeder validation check
		RequestCustomHeaderInfo requestLoginHeaderInfo = getSessionRequestCustomHeaderInfo(request);
		
		logger.info("requestLoginHeaderInfo : "+requestLoginHeaderInfo.toString());
		
		if(!requestLoginHeaderInfo.getErrorCode().equals(CodeMessage.RESPONSE_CODE_OK)) {
        	ResultCode result = CodeMessage.setResultCode(requestLoginHeaderInfo.getErrorCode());
        	rm.setResult(result);
		} else {
			rm = memberService.logoutMember(requestLoginHeaderInfo);
		}
		
		return rm;
	}
	
	/*
	 * 이메일 인증
	 */
	@RequestMapping(value = "/emailAuth", method = RequestMethod.GET)
	@ResponseBody
	public ModelAndView emailAuth(HttpServletRequest request, @ModelAttribute @Valid EmailAuthInfo emailAuthInfo, BindingResult bindingResult) {
		ResultMaster rm = new ResultMaster();
		ModelAndView modelAndView = new ModelAndView("/emailAuthResult");
		
		if(bindingResult.hasErrors() || StringUtils.nvlStr(emailAuthInfo.getEmailAuthKey()).equals("")) {
        	ResultCode result = CodeMessage.setResultCode(CodeMessage.RESPONSE_CODE_NOT_PARAMETER);
        	rm.setResult(result);
		} else {
			rm = memberService.emailAuth(emailAuthInfo);
		}
		
		if (rm.getResult().getCode().equals(CodeMessage.RESPONSE_CODE_OK)) {
			modelAndView.addObject("msg",  ConfigProperties.getProperty("email.auth.succ.msg"));
		} else {
			modelAndView.addObject("msg",  ConfigProperties.getProperty("email.auth.fail.msg"));
		}
		
		return modelAndView;
	}
	
	/*
	 * 아이디 중복체크 
	 */
	 @RequestMapping(value = "/checkId", method = RequestMethod.POST, produces="application/json")
	 public @ResponseBody ResultMaster checkId(HttpServletRequest request) throws Exception
	 {
		 ResultMaster rm = null;
		 HttpRequest httpRequest = new HttpRequest(request);
		 Map<String, Object> param = httpRequest.getParameterNameAndValueMap();
		 
		 rm = memberService.checkId(param);
		 
		 return rm;
	 }
	 
	/*
	 * 회원 비밀번호 변경
	 */
	@RequestMapping(value = "/modifyPwd", method = RequestMethod.PUT, produces="application/json")
	@ResponseBody
	public ResultMaster modifyPwd(HttpServletRequest request, @ModelAttribute @Valid ModifyPwdInfo modifyPwdInfo, BindingResult bindingResult) {
		ResultMaster rm = new ResultMaster();
		
		// haeder validation check
		RequestCustomHeaderInfo requestLoginHeaderInfo = getSessionRequestCustomHeaderInfo(request);
		
		if((!requestLoginHeaderInfo.getErrorCode().equals(CodeMessage.RESPONSE_CODE_OK)) || bindingResult.hasErrors()) {
        	ResultCode result = CodeMessage.setResultCode(requestLoginHeaderInfo.getErrorCode());
        	rm.setResult(result);
		} else {
			rm = memberService.modifyPwd(modifyPwdInfo, requestLoginHeaderInfo.getMemNo());
		}
		
		return rm;
	}
	
	/*
	 * 회원 비밀번호 변경
	 */
	@RequestMapping(value = "/getTempPwd", method = RequestMethod.GET, produces="application/json")
	@ResponseBody
	public ResultMaster getTempPwd(HttpServletRequest request) {
		ResultMaster rm = new ResultMaster();
		
		// not login haeder validation check
		RequestCustomHeaderInfo requestCustomHeaderInfo = geNotLoginRequestHeaderInfo(request);
		
		if((!requestCustomHeaderInfo.getErrorCode().equals(CodeMessage.RESPONSE_CODE_OK))) {
        	ResultCode result = CodeMessage.setResultCode(CodeMessage.RESPONSE_CODE_NOT_PARAMETER);
        	rm.setResult(result);
		} else {
			HttpRequest httpRequest = new HttpRequest(request);
			Map<String, Object> param = httpRequest.getParameterNameAndValueMap();
			 
			rm = memberService.getTempPwd(param);
		}
		
		return rm;
	}

	
	/*
	 * 닉네임 중복체크 
	 */
	 @RequestMapping(value = "/checkNickName", method = RequestMethod.POST, produces="application/json")
	 public @ResponseBody ResultMaster checkNickName(HttpServletRequest request) throws Exception
	 {
		 ResultMaster rm = null;
		 HttpRequest httpRequest = new HttpRequest(request);
		 Map<String, Object> param = httpRequest.getParameterNameAndValueMap();
		 
		 rm = memberService.checkNickName(param);
		 
		 return rm;
	 }
}
