package com.uws.evaluation.service;

import java.util.List;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;

public interface IEvaluationScoreService {
	/***
	 * 查询综合测评记录
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryEvaluationScorePage(int pageNum, int pageSize, EvaluationInfo evaluation);
	
	/***
	 * 综合测评 下一个 评分功能
	 * @param evaluation
	 * @return
	 */
	public EvaluationInfo getNextEvaluation(EvaluationInfo evaluation);
	
	/***
	 * 测评分数导入
	 * @param list
	 */
	public void importData(List<EvaluationDetail> list);
	
	/***
	 * 测评旧数据导入
	 * @param list
	 */
	public void importEvaluationData(List<EvaluationInfo> list);
	
	/***
	 * 查询已确认综合测评记录
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryConfirmEvaluationList(int pageNum, int pageSize, EvaluationInfo evaluation);
	
	/***
	 * 获取当前登录人所维护的班级
	 * @param userId
	 * @return
	 */
	public List<BaseClassModel> queryEvaluationClassList(String userId);
	
	/***
	 * 生成该yearId、termId、monthId、下班级所有成员的测评记录 
	 * @param yearId
	 * @param termId
	 * @param monthId
	 * @param classId
	 */
	public void addClassEvaluation(String yearId, String termId, String monthId, String classId);

}
