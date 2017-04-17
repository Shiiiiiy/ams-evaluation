package com.uws.evaluation.service.impl;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.common.service.IStudentCommonService;
import com.uws.core.base.BaseServiceImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.evaluation.EvaluationUser;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.dao.IStuEvaluationDao;
import com.uws.evaluation.service.IEvaluationSetService;
import com.uws.evaluation.service.IStuEvaluationService;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

@Service("stuEvaluationservice")
public class StuEvaluationServiceImpl extends BaseServiceImpl implements IStuEvaluationService {
	
	@Autowired
	private IStuEvaluationDao stuEvaluationDao;
	
	@Autowired
	private IStudentCommonService studentCommonService;
	
	@Autowired
	private IEvaluationSetService evaluationSetService;
	
	@Autowired
	private IStuJobTeamSetCommonService jobTeamService;
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	/***
	 * 学生测评维护列表页查询
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation){
		return this.stuEvaluationDao.queryEvaluationPage(pageNum, pageSize, evaluation);
	}
	
	/***
	 * 通过userID查询对应学院现阶段可测评的月份及对应时间
	 * @param userId
	 * @return
	 */
	public List<EvaluationTime> getEvaluationTimeByUserId(String userId){
		String collegeId="";
		StudentInfoModel student=this.studentCommonService.queryStudentById(userId);
		if(DataUtil.isNotNull(student)){//学生
			return this.stuEvaluationDao.getEvaluationTimeByCollegeId(student.getMajor().getCollage().getId());
		}else{//教师（测评辅导员、班主任）
			List<BaseAcademyModel> list=this.jobTeamService.getBAMByTeacherId(userId);
			if(list.size()>0){
				collegeId=list.get(0).getCode();
			}
		}
		
		return this.stuEvaluationDao.getEvaluationTimeByCollegeId(collegeId);
	}

	/**
	 * 获取字典
	 */
	public Dic getDicById(String id){
		return this.stuEvaluationDao.getDicById(id);
	}
	
	/***
	 * 获取个人测评信息
	 */
	public EvaluationInfo getEvaluationInfo(String year, String month, String user){
		return this.stuEvaluationDao.getEvaluationInfo(year, month, user);
	}
	
	/**
	 * 获取个人测评信息明细
	 */
	public List<EvaluationDetail> getEvaluationDetailById(String id){
		return this.stuEvaluationDao.getEvaluationDetailListByEvaluationId(id);
	}
	
	/***
	 * 保存综合测评信息
	 */
	public void saveEvaluation(HttpServletRequest request, String command){
		EvaluationInfo evaluation=new EvaluationInfo();
		String yearId=request.getParameter("yearId");
		String termId=request.getParameter("termId");
		String monthId=request.getParameter("monthId");
		String studentId=request.getParameter("studentId");
		String[] ids=request.getParameterValues("ids");
		String[] reasons=request.getParameterValues("reasons");
		
		List<Dic> baseTypeList=this.dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		Dic statusDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", command);
		
		Dic yearDic=new Dic();
		yearDic.setId(yearId);
		evaluation.setYear(yearDic);
		
		Dic termDic=new Dic();
		termDic.setId(termId);
		evaluation.setTerm(termDic);
		
		Dic monthDic=new Dic();
		monthDic.setId(monthId);
		evaluation.setMonth(monthDic);
		
		StudentInfoModel student=this.studentCommonService.queryStudentById(studentId);
		evaluation.setStudent(student);
		
		EvaluationUser evaluationUser = this.evaluationSetService.queryEvaluationUser(student.getClassId().getId());
		if(null != evaluationUser){
			evaluation.setAssist(evaluationUser.getAssist());
		}
		
		evaluation.setStatus(statusDic);//保存状态
		evaluation.setMoralScoreSum("0");
		evaluation.setCapacityScoreSum("0");
		evaluation.setCultrueScoreSum("0");
		evaluation.setIntellectScoreSum("0");
		
		this.stuEvaluationDao.saveEvaluation(evaluation);//保存测评基础信息
		
		int seqNum=0;
		
		for (int i = 0; i < (ids.length/baseTypeList.size()); i++) {
			for(int j=0;j<baseTypeList.size();j++,seqNum++){
				Dic dic = (Dic) baseTypeList.get(j);
				EvaluationDetail detail=new EvaluationDetail();
				detail.setReason(reasons[baseTypeList.size()*i+j].trim());
				detail.setEvaluation(evaluation);
				detail.setType(dic);
				detail.setSeqNum(seqNum);
				this.stuEvaluationDao.saveEvaluationDetail(detail);
			}
		}
		
	}
	
	/****
	 * 更新综合测评信息
	 */
	public void updateEvaluation(String id, HttpServletRequest request, String command){
		String studentId=request.getParameter("studentId");
		String[] ids=request.getParameterValues("ids");
		String[] reasons=request.getParameterValues("reasons");
		String[] scores=request.getParameterValues("scores");
		String intellectScoreSum=request.getParameter("intellectScoreSum");//智育分
		String moralScoreSum=request.getParameter("moralScoreSum");//德育分
		String capacityScoreSum=request.getParameter("capacityScoreSum");//能力分
		String cultrueScoreSum=request.getParameter("cultrueScoreSum");//文体分
		String sumScores=request.getParameter("sumScores");//测评总分
		
		List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		List<Dic> scoreTypeList=dicUtil.getDicInfoList("EVALUATION_SCORE_TYPE");//测评分分数类型 
		
		Dic statusDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", command);
		
		EvaluationInfo evaluation=this.getEvaluationInfoById(id);
		
		StudentInfoModel student=this.studentCommonService.queryStudentById(studentId);
		//更新当前测评员
		EvaluationUser evaluationUser = this.evaluationSetService.queryEvaluationUser(student.getClassId().getId());
		if(null != evaluationUser){
			evaluation.setAssist(evaluationUser.getAssist());
		}
		
		evaluation.setStatus(statusDic);//更新状态
		if("TO_CONFIRMED".equals(command) || "CONFIRMED".equals(command)){
			evaluation.setMoralScoreSum(moralScoreSum);
			evaluation.setCultrueScoreSum(cultrueScoreSum);
			evaluation.setCapacityScoreSum(capacityScoreSum);
			evaluation.setIntellectScoreSum(intellectScoreSum);
			evaluation.setScoreSum(sumScores);
		}
		
		this.stuEvaluationDao.updateEvaluation(evaluation);//更新测评信息
		this.stuEvaluationDao.deleteEvaluationDetailByIds(id, ids);//删除已删掉的明细记录
		
		int seqNum=0;
		
		for (int i = 0; i < (ids.length/baseTypeList.size()); i++) {
			for(int j=0;j<baseTypeList.size();j++,seqNum++){
				if(DataUtil.isNotNull(ids[baseTypeList.size()*i+j])){//更新
					EvaluationDetail detail=this.stuEvaluationDao.getEvaluationDetailById(ids[baseTypeList.size()*i+j]);
					detail.setReason(reasons[baseTypeList.size()*i+j].trim());
					detail.setScore(scores[baseTypeList.size()*i+j]);
					detail.setSeqNum(seqNum);
					this.stuEvaluationDao.updateEvaluationDetail(detail);
				}else{//新增
					Dic dic = (Dic) baseTypeList.get(j);
					
					EvaluationDetail detail=new EvaluationDetail();
					detail.setReason(reasons[baseTypeList.size()*i+j].trim());
					detail.setScore(scores[baseTypeList.size()*i+j]);
					detail.setEvaluation(evaluation);
					detail.setType(dic);
					detail.setSeqNum(seqNum);
					this.stuEvaluationDao.saveEvaluationDetail(detail);
				}
			}
		}
	}
	
	/*****
	 * 查询综合测评信息
	 * @param id
	 * @return
	 */
	public EvaluationInfo getEvaluationInfoById(String id){
		return this.stuEvaluationDao.getEvaluationInfoById(id);
	}
	
	/****
	 * 删除测评信息
	 */
	public void deleteEvaluationById(String id){
		this.stuEvaluationDao.deleteEvaluationById(id);
	}
	
	
	/****
	 * 学生确认测评信息分数
	 */
	public void confirmEvaluation(String id){
		Dic statusDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "CONFIRMED");
		EvaluationInfo evaluation=this.getEvaluationInfoById(id);
		evaluation.setStatus(statusDic);//更新状态
		
		this.stuEvaluationDao.updateEvaluation(evaluation);//更新测评信息
	}
}
