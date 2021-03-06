package com.wt.openapi.member.dao;

import java.util.HashMap;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.wt.openapi.member.model.info.EmailAuthInfo;
import com.wt.openapi.member.model.vo.MemberInfo;
import com.wt.openapi.member.model.vo.MemberPwdInfo;

@Repository
public class MemberDaoImpl implements MemberDao{
	
	public static String NAMESPACE = "wt.member.";
	
	@Autowired
    private SqlSession sqlSession;
 
    public void setSqlSession(SqlSession sqlSession){
        this.sqlSession = sqlSession;
    }
 
    @Override
	public int joinMember(MemberInfo memberInfo) throws Exception {
    	return sqlSession.insert(NAMESPACE + "joinMember", memberInfo);
	}

    @Override
	public int insertPwd(MemberPwdInfo memberPwdInfo) throws Exception {
		return sqlSession.insert(NAMESPACE + "insertPwd", memberPwdInfo);
	}

	@Override
	public MemberInfo selectMember(HashMap<String, Object> param) throws Exception {
		return sqlSession.selectOne(NAMESPACE + "selectMember", param);
	}

	@Override
	public MemberPwdInfo selectPwd(String memNo) throws Exception {
		return sqlSession.selectOne(NAMESPACE + "selectPwd", memNo);
	}

	@Override
	public int updateEmailAuth(EmailAuthInfo emailAuthInfo) throws Exception {
		return sqlSession.update(NAMESPACE + "updateEmailAuth", emailAuthInfo);
	}
	
	@Override
	public int checkId(String userId)
	{
		return sqlSession.selectOne(NAMESPACE + "checkId", userId);
	}
	
	@Override
	public int modifyPwd(MemberPwdInfo newPwdInfo) throws Exception {
		return sqlSession.update(NAMESPACE + "modifyPwd", newPwdInfo);
	}
	
	@Override
	public int checkNickName(String userNickName)
	{
		return sqlSession.selectOne(NAMESPACE + "checkNickName", userNickName);
	}
}