package com.uws.evaluation.dao;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.orientation.StudentInfoModel;

public interface IEvaluationStatisticDao {
	
	/***
	 * 测评统计
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page statisticEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request);
	
	/***
	 * 根据学年、学生查询该学年这学生的测评成绩综合及各成绩的排名
	 * @param yearId
	 * @param student
	 * @return已转移到公共类中
	 */
	//public Map<String,String> queryStudentEvaluationScore(String yearId, StudentInfoModel student);

}
