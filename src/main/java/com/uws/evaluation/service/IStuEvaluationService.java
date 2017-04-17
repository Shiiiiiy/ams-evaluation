package com.uws.evaluation.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.sys.model.Dic;

public interface IStuEvaluationService {
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
	public List<EvaluationTime> getEvaluationTimeByUserId(String userId);

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
	 * 通过测评ID查询测评信息
	 * @param id
	 * @return
	 */
	public EvaluationInfo getEvaluationInfoById(String id);
	
	/***
	 * 通过测评id查询对应的测评明细
	 * @param id
	 * @return
	 */
	public List<EvaluationDetail> getEvaluationDetailById(String id);
	
	/***
	 * 保存测评记录 
	 * @param request
	 * @param command
	 */
	public void saveEvaluation(HttpServletRequest request, String command);
	
	/***
	 * 更新测评记录
	 * @param id
	 * @param request
	 * @param command
	 */
	public void updateEvaluation(String id, HttpServletRequest request, String command);
	
	/***
	 * 删除测评记录
	 * @param id
	 */
	public void deleteEvaluationById(String id);
	
	/***
	 * 学生确认测评记录
	 * @param id
	 */
	public void confirmEvaluation(String id);
}
