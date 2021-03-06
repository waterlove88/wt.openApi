package com.wt.openapi.member.service;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.wt.openapi.common.model.RequestCustomHeaderInfo;
import com.wt.openapi.common.model.ResultMaster;
import com.wt.openapi.member.model.info.EmailAuthInfo;
import com.wt.openapi.member.model.info.JoinMemberInfo;
import com.wt.openapi.member.model.info.LoginMemberInfo;
import com.wt.openapi.member.model.info.ModifyPwdInfo;

public interface MemberService {
	ResultMaster joinMember(JoinMemberInfo joinMemberInfo);
	ResultMaster loginMember(LoginMemberInfo loginMemberInfo, RequestCustomHeaderInfo requestNotLoginHeaderInfo, HttpServletResponse response);
	ResultMaster logoutMember(RequestCustomHeaderInfo requestLoginHeaderInfo);
	ResultMaster emailAuth(EmailAuthInfo emailAuthInfo);
	ResultMaster checkId(Map<String, Object> param);
	ResultMaster modifyPwd(ModifyPwdInfo modifyPwdInfo, String memNo);
	ResultMaster getTempPwd(Map<String, Object> param);
	ResultMaster checkNickName(Map<String, Object> param);
}
