package com.uws.evaluation.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;

import com.uws.core.base.BaseServiceImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.util.DataUtil;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationInfoVo;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.dao.IEvaluationQueryDao;
import com.uws.evaluation.dao.IStuEvaluationDao;
import com.uws.evaluation.service.IEvaluationQueryService;

@Service("evaluationQueryService")
public class EvaluationQueryServiceImpl extends BaseServiceImpl implements IEvaluationQueryService {
	
	@Autowired
	private IEvaluationQueryDao evaluationQueryDao;
	
	@Autowired
	private IStuEvaluationDao stuEvaluationDao;
	
	/***
	 * 查询已确认的测评记录
	 */
	public Page queryEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request){
		return this.evaluationQueryDao.queryEvaluationPage(pageNum, pageSize, evaluation, request);
	}
	
	/****
	 * 查询班级测评信息
	 */
	public Page queryClassEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request){
		return this.evaluationQueryDao.queryClassEvaluationPage(pageNum, pageSize, evaluation, request);
	}
	
	/****
	 * 获取班级的所有测评明细
	 */
	public void getClassEvaluationDetail(EvaluationInfo evaluation, ModelMap model){
		List<StudentInfoModel> studentList=new ArrayList();
		Map<String,EvaluationInfoVo> studentMap=new HashMap<String,EvaluationInfoVo>();
		Map<String,List<EvaluationDetail>> evaluationMap=new HashMap<String,List<EvaluationDetail>>();
		
		if(DataUtil.isNotNull(evaluation)){
			String studentId=evaluation.getStudent().getClassId().getId();
			studentList=this.evaluationQueryDao.queryStudentInfoByClassId(studentId);
			for (Iterator iterator = studentList.iterator(); iterator.hasNext();) {//遍历所有学生
				StudentInfoModel studentInfoModel = (StudentInfoModel) iterator.next();
				evaluation.setStudent(studentInfoModel);
				List<EvaluationInfoVo> evaluationList=this.evaluationQueryDao.getStudentEvaluation(evaluation);
				if(DataUtil.isNotNull(evaluationList) && evaluationList.size()>0){
					List<EvaluationDetail> detailList=this.stuEvaluationDao
							.getEvaluationDetailListByEvaluationId(evaluationList.get(0).getId());
					studentMap.put(studentInfoModel.getId(), evaluationList.get(0));
					evaluationMap.put(evaluationList.get(0).getId(), detailList);
				}
			}
		}
		model.addAttribute("studentList", studentList);
		model.addAttribute("studentMap", studentMap);
		model.addAttribute("evaluationMap", evaluationMap);
	}
	
	/***
	 * 查询班级单月测评明细
	 * @param evaluation
	 * @return
	 */
	public List<EvaluationInfoVo> queryClassEvaluationList(EvaluationInfo evaluation){
		return this.evaluationQueryDao.queryClassEvaluationList(evaluation);
	}

	/***
	 * 查询学生单月测评记录（用于测评导出月明细）
	 * @param id
	 * @return
	 */
	public Map<String,String> queryMonthEvaluationDetail(String id){
		Map<String,String> studentMap = new HashMap<String,String>();
		List<Object[]> detailList = this.evaluationQueryDao.queryMonthEvaluationDetail(id);
		if(detailList != null){
			for (Object[] objects : detailList) {
				if(DataUtil.isNotNull(objects)){
					studentMap.put(objects[1] != null?objects[1].toString():"", objects[2]!= null?objects[2].toString():"");
				}
			}
		}
		return studentMap;
	}
}
