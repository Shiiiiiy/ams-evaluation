package com.uws.evaluation.service.impl;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uws.core.base.BaseServiceImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.dao.IEvaluationStatisticDao;
import com.uws.evaluation.service.IEvaluationStatisticService;

@Service("evaluationStatisticService")
public class EvaluationStatisticServiceImpl extends BaseServiceImpl implements IEvaluationStatisticService {
	@Autowired
	public IEvaluationStatisticDao evaluationStatisticDao;
	
	
	/****
	 * 综合测评统计
	 */
	public Page statisticEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request){
		return this.evaluationStatisticDao.statisticEvaluationPage(pageNum, pageSize, evaluation, request);
	}
	
	/***
	 * 根据学年、学生查询该学年这学生的测评成绩综合及各成绩的排名
	 * @param yearId
	 * @param student
	 * @return已转移到公共类中
	 */
	/*public Map<String,String> queryStudentEvaluationScore(String yearId, StudentInfoModel student){
		return this.evaluationStatisticDao.queryStudentEvaluationScore(yearId, student);
	}*/

}
