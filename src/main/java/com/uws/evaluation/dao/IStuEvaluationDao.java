package com.uws.evaluation.dao;

import java.util.List;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.sys.model.Dic;

public interface IStuEvaluationDao {
	/***
	 * 学生测评维护列表页查询
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation);
	
	/***
	 * 通过userID查询对应学院现阶段可测评的月份及对应时间
	 * @param userId
	 * @return
	 */
	public List<EvaluationTime> getEvaluationTimeByCollegeId(String userId);
	
	/***
	 * 通过字典ID查询对应字典信息
	 * @param id
	 * @return
	 */
	public Dic getDicById(String id);
	
	/***
	 * 通过学年测评、月份、用户查询测评记录
	 * @param year
	 * @param month
	 * @param user
	 * @return
	 */
	public EvaluationInfo getEvaluationInfo(String year, String month, String user);
	
	/***
	 * 通过测评id查询对应的测评明细
	 * @param id
	 * @return
	 */
	public List<EvaluationDetail> getEvaluationDetailListByEvaluationId(String id);
	
	/***
	 * 保存测评记录 
	 * @param request
	 * @param command
	 */
	public void saveEvaluation(EvaluationInfo evaluation);
	
	/***
	 * 通过测评ID查询测评信息
	 * @param id
	 * @return
	 */
	public EvaluationInfo getEvaluationInfoById(String id);
	
	/***
	 * 更新测评记录
	 * @param id
	 * @param request
	 * @param command
	 */
	public void updateEvaluation(EvaluationInfo evaluation);
	
	/***
	 * 保存测评记录 
	 * @param request
	 * @param command
	 */
	public void saveEvaluationDetail(EvaluationDetail detail);
	
	/***
	 * 更新测评明细
	 * @param id
	 * @param request
	 * @param command
	 */
	public void updateEvaluationDetail(EvaluationDetail detail);
	
	/***
	 * 通过测评ID查询测评信息
	 * @param id
	 * @return
	 */
	public EvaluationDetail getEvaluationDetailById(String id);
	
	/***
	 * 删除测评明细
	 * @param id
	 * @param detailIds
	 */
	public void deleteEvaluationDetailByIds(String id, String[] detailIds);
	
	/***
	 *删除测评记录
	 * @param id
	 */
	public void deleteEvaluationById(String id);

}
