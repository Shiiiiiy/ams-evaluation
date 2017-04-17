package com.uws.evaluation.dao;

import java.util.List;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;

public interface IEvaluationScoreDao {
	
	/***
	 * 查询测评记录
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryEvaluationScorePage(int pageNum, int pageSize, EvaluationInfo evaluation);
	
	/***
	 * 测评评分  下一个  查询
	 * @param evaluation
	 * @return
	 */
	public  List<EvaluationInfo> getNextEvaluation(EvaluationInfo evaluation);
	
	/***
	 * 通过测评记录Id\类型Id\测评事由查询对应的测评明细   为导入分数功能
	 * @param evaluationId
	 * @param typeId
	 * @param reason
	 * @return
	 */
	public List<EvaluationDetail> getEvaluationDetail(String evaluationId, String typeId, String reason);
	
	/***
	 * 通过导入修改测评分数
	 * @param id
	 * @param score
	 */
	public void updateEvaluationDetailScore(EvaluationDetail evaluationDetail);
	
	/***
	 * 通过测评记录id查询明细总分
	 * @param id
	 * @return
	 */
	public List queryEvaluationSumScore(String id);
	
	/***
	 * 查询基础设置的值
	 * @param baseTypeId
	 * @param scoreTypeId
	 * @return
	 */
	public String getBaseScore(String baseTypeId, String scoreTypeId);

	/***
	 * 查询已确认综合测评记录
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryConfirmEvaluationList(int pageNum, int pageSize, EvaluationInfo evaluation);
	
	/***
	 * 通过测评员查询所管理的班级
	 * @param userId
	 * @return
	 */
	public List<BaseClassModel> getClassByEvaluationUser(String userId);
}
