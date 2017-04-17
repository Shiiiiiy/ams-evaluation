package com.uws.evaluation.dao;

import java.util.List;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTerm;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.evaluation.EvaluationUser;

/**
 * @Description 综合测评基础Dao接口
 * @author Jiangbl
 * @date 2015-8-13
 */

public interface IEvaluationSetDao {
	public List<EvaluationScore> queryEvaluationScore();
	
	public void saveEvaluationScore(EvaluationScore evaluationScore);
	
	public void updateEvaluationScore(EvaluationScore evaluationScore);
	
	public EvaluationScore getEvaluationScoreById(String id);
	
	public Page queryEvaluationTimePage(int pageNum, int pageSize, EvaluationTime evaluationTime, String currentUserId);
	
	public void saveEvaluationTime(EvaluationTime evaluationTime);
	
	public void updateEvaluationTime(EvaluationTime evaluationTime);
	
	public EvaluationTime getEvaluationTimeById(String id);
	
	public void deleteEvaluationTimeById(String id);
	
	public Boolean getEvaluationTime(String collegeId, String monthId, String id);
	
	public Page queryClassEvaluationUserPage(int pageNum, int pageSize, EvaluationUser evaluationUser);
	
	public EvaluationUser queryEvaluationUser(String classId);
	
	public void saveEvaluationUser(EvaluationUser user);
	
	public void updateEvaluationUser(EvaluationUser user);
	
	/***
	 * 通过userId查询该用户所管测评的所有班级
	 * @param userId
	 */
	public List<EvaluationUser> getEvaluationUserListByUserId(String userId);

	/***
	 * 查询测评基础学期设置
	 * @return
	 */
	public List<EvaluationTerm> queryEvaluationTerm();
	
	/***
	 * 删除测评学期设置
	 */
	public void deleteEvaluationTerm();
	
	/***
	 * 保存学期设置
	 * @param evaluationTerm
	 */
	public void saveEvaluationTerm(EvaluationTerm evaluationTerm);
}
