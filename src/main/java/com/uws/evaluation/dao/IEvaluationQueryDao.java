package com.uws.evaluation.dao;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationInfoVo;
import com.uws.domain.orientation.StudentInfoModel;

public interface IEvaluationQueryDao {
	/***
	 * 个人测评查询
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @param request
	 * @return
	 */
	public Page queryEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request);
	
	/***
	 * 班级测评查询
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @param request
	 * @return
	 */
	public Page queryClassEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request);

	/***
	 * 查询班级下的所有学生
	 * @param klassId
	 * @param province
	 * @param flag
	 * @return
	 */
	public List<StudentInfoModel> queryStudentInfoByClassId(String classId);
	
	/***
	 *  通过班级id、学年、学期、月份查询学生的测评明细
	 * @param evaluation
	 * @return
	 */
	public List<EvaluationInfoVo> getStudentEvaluation(EvaluationInfo evaluation);
	
	/***
	 *  通过班级id、学年、学期、月份查询班级单月的测评
	 * @param evaluation
	 * @return
	 */
	public List<EvaluationInfoVo> queryClassEvaluationList(EvaluationInfo evaluation);

	/***
	 * 查询学生单月测评记录（用于测评导出月明细）
	 * @param id
	 * @return
	 */
	public List queryMonthEvaluationDetail(String id);
}
