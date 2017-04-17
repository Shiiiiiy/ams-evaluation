package com.uws.evaluation.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTerm;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.evaluation.EvaluationUser;

/**
 * @Description 综合测评基础设置Service接口
 * @author Jiangbl
 * @date 2015-8-13
 */

public interface IEvaluationSetService {
	
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
	
	public void saveEvaluationUser(String studentId, String classId);
	
	public void updateEvaluationUser(EvaluationUser user, String studentId);
	
	/***
	 * 查询测评基础学期设置
	 * @return
	 */
	public List<EvaluationTerm> queryEvaluationTerm();
	
	
	/***
	 * 保存测评学期设置
	 * @param request
	 */
	public void saveEvaluationTerm(HttpServletRequest request);
}
