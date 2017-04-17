package com.uws.evaluation.service;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.ModelMap;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationInfoVo;

public interface IEvaluationQueryService {

	/***
	 * 查询单个学生的测评信息
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request);
	
	/***
	 * 查询班级的测评信息
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryClassEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request);
	
	/***
	 * 查询班级测评明细
	 * @param evaluation
	 * @param model
	 */
	public void getClassEvaluationDetail(EvaluationInfo evaluation, ModelMap model);
	
	/***
	 * 查询班级单月测评明细
	 * @param evaluation
	 * @return
	 */
	public List<EvaluationInfoVo> queryClassEvaluationList(EvaluationInfo evaluation);
	
	/***
	 * 查询学生单月测评记录（用于测评导出月明细）
	 * @param id
	 * @return
	 */
	public Map<String,String> queryMonthEvaluationDetail(String id);
}
