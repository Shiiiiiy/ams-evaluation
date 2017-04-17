package com.uws.evaluation.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uws.common.dao.IBaseDataDao;
import com.uws.common.dao.IStudentCommonDao;
import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.core.base.BaseServiceImpl;
import com.uws.core.hibernate.dao.impl.BaseDaoImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationUser;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.dao.IEvaluationQueryDao;
import com.uws.evaluation.dao.IEvaluationScoreDao;
import com.uws.evaluation.dao.IEvaluationSetDao;
import com.uws.evaluation.dao.IStuEvaluationDao;
import com.uws.evaluation.service.IEvaluationScoreService;
import com.uws.evaluation.service.IEvaluationSetService;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.user.model.User;

@Service("evaluationScoreService")
public class EvaluationScoreServiceImpl extends BaseDaoImpl implements IEvaluationScoreService {
	@Autowired
	private IEvaluationScoreDao evaluationScoreDao;
	
	@Autowired
	private IStudentCommonDao studentCommonDao;
	
	@Autowired
	private IEvaluationQueryDao evaluationQueryDao;
	
	@Autowired
	private IStuEvaluationDao stuEvaluationDao;
	
	@Autowired
	private IBaseDataDao baseDataDao;
	
	@Autowired
	private IEvaluationSetService evaluationSetService;
	
	@Autowired
	private IStuJobTeamSetCommonService jobTeamService;
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	@Autowired
	private DicUtil dicUtil = DicFactory.getDicUtil();
	
	/***
	 * 查询已提交的测评记录
	 */
	public Page queryEvaluationScorePage(int pageNum, int pageSize, EvaluationInfo evaluation){
		return this.evaluationScoreDao.queryEvaluationScorePage(pageNum, pageSize, evaluation);
	}
	
	/***
	 * 查询下一个测评记录
	 */
	public EvaluationInfo getNextEvaluation(EvaluationInfo evaluation){
		EvaluationInfo nextEvaluation=new EvaluationInfo();
		List<EvaluationInfo> list=this.evaluationScoreDao.getNextEvaluation(evaluation);
		if(list.size()>1){
			for (int i=0;i<2;i++) {
				if(!evaluation.getStudent().getId().equals(list.get(i).getStudent().getId())){
					nextEvaluation=list.get(i);
					break;
				}
			}
		}
		
		return nextEvaluation;
	}
	
	/***
	 * 测评分数导入:只针对分数做修改
	 * 通过学生、学年、测评月份查出学生的测评记录
	 * 通过测评记录和测评类别及事由查出对应的测评明细
	 */
	public void importData(List<EvaluationDetail> list){
		String yearId = "";
	    String termId = "";
	    String monthId = "";
	    String typeId = "";
	   // List<EvaluationInfo> evaluationList=new ArrayList();
	    Set<EvaluationInfo> evaluationList=new HashSet<EvaluationInfo>();
	    List<Dic> typeDic = this.dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");
		
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			EvaluationDetail evaluationDetail = (EvaluationDetail) iterator.next();
			
			 String reason = evaluationDetail.getReason();
			 String score = evaluationDetail.getScore();
			
			EvaluationInfo evaluation=new EvaluationInfo();
			StudentInfoModel student = this.studentCommonDao.queryStudentByStudentNo(evaluationDetail.getStudentNo());
			List<Dic> yearDic = this.dicUtil.getDicInfoList("YEAR");
			List<Dic> termDic = this.dicUtil.getDicInfoList("TERM");
			List<Dic> monthDic = this.dicUtil.getDicInfoList("MONTH");
			
			for (Dic dic : yearDic){
				if (evaluationDetail.getYearId().equals(dic.getName())) {
					yearId = dic.getId();
					break;
				}
			}
				
			for (Dic dic : termDic){
				if (evaluationDetail.getTermId().equals(dic.getName())) {
					termId = dic.getId();
					break;
				}
			}
			
			for (Dic dic : monthDic){
				if (evaluationDetail.getMonthId().equals(dic.getName())) {
					monthId = dic.getId();
					break;
				}
			}
			if(DataUtil.isNotNull(student)){
				//查询测评记录
				evaluation=this.stuEvaluationDao.getEvaluationInfo(yearId, monthId, student.getId());
				if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getId())){
					for (Dic dic : typeDic){
						if (evaluationDetail.getTypeId().equals(dic.getName())) {
							typeId = dic.getId();
							break;
						}
					}
					List<EvaluationDetail> evaluationDetailList = this.evaluationScoreDao.getEvaluationDetail(evaluation.getId(), typeId, reason);
					for (Iterator iterator2 = evaluationDetailList.iterator(); iterator2.hasNext();) {
						EvaluationDetail evaluationDetail2 = (EvaluationDetail) iterator2.next();
						//修改测评明细分数
						evaluationDetail2.setScore(score);
						this.evaluationScoreDao.updateEvaluationDetailScore(evaluationDetail2);
						evaluationList.add(evaluation);
					}
					
				}else{
					System.out.println("不存在该测评记录");
				}
			}
		}
		
		this.flush();
		this.updateEvaluation(evaluationList,typeDic);
	}
	
	/***
	 * 导入测评分数后统计总分、修改测评记录的状态
	 * @param list
	 * @param typeDic
	 */
	//private void updateEvaluation(List<EvaluationInfo> list, List<Dic> typeDic){
	private void updateEvaluation(Set<EvaluationInfo> list, List<Dic> typeDic){
		User creator=new User(this.sessionUtil.getCurrentUserId());//当前登录人
		Dic statusDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "TO_CONFIRMED");
		Dic comfinDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "CONFIRMED");
		Dic baseScoreDic=this.dicUtil.getDicInfo("EVALUATION_SCORE_TYPE", "BASE_SCORE");
		Dic baseWeightDic=this.dicUtil.getDicInfo("EVALUATION_SCORE_TYPE", "WEIGHT");
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			//double sumScore=0;
			double intellectWeight=1;
			EvaluationInfo evaluationInfo = (EvaluationInfo) iterator.next();
			//已确认状态的不能修改
			if(!(comfinDic.getId()).equals(evaluationInfo.getStatus().getId())){
				//获取该记录的各类别的测评总分
				List<Object[]> scoreList = this.evaluationScoreDao.queryEvaluationSumScore(evaluationInfo.getId());
				
				for (Iterator iterator2 = scoreList.iterator(); iterator2.hasNext();) {
					Object[] objects = (Object[]) iterator2.next();
						//获取类别
						Dic dic=this.stuEvaluationDao.getDicById(objects[0].toString());
						
						if("MORAL".equals(dic.getCode())){
							int  moralScore = Integer.parseInt(objects[1]!=null?objects[1].toString():"0");
							evaluationInfo.setMoralScoreSum(String.valueOf(moralScore));
						}else if("CULTURE".equals(dic.getCode())){
							int  cultrueScore = Integer.parseInt(objects[1]!=null?objects[1].toString():"0");
							evaluationInfo.setCultrueScoreSum(String.valueOf(cultrueScore));
						}else if("CAPACITY".equals(dic.getCode())){
							int  capatityScore = Integer.parseInt(objects[1]!=null?objects[1].toString():"0");
							evaluationInfo.setCapacityScoreSum(String.valueOf(capatityScore));
						}
				}
				/** 单独获取智育的分数*/	
				int  intellectScore = 99;
				evaluationInfo.setIntellectScoreSum(String.valueOf(intellectScore));
				
				evaluationInfo.setStatus(statusDic);
				evaluationInfo.setAssist(creator);
				this.stuEvaluationDao.updateEvaluation(evaluationInfo);
			}
		}
		
	}
	
	
	/***
	 * 测评旧数据导入
	 * @param list
	 */
	public void importEvaluationData(List<EvaluationInfo> list){
		
	   // List<EvaluationInfo> evaluationList=new ArrayList();
	    Set<EvaluationInfo> evaluationList=new HashSet<EvaluationInfo>();
	    List<Dic> typeDic = this.dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");
	    List<Dic> yearDic = this.dicUtil.getDicInfoList("YEAR");
		List<Dic> termDic = this.dicUtil.getDicInfoList("TERM");
		List<Dic> monthDic = this.dicUtil.getDicInfoList("MONTH");
		Dic statusDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "CONFIRMED");
	    
		for (int i=0;i<list.size();i=i+3) {
			EvaluationInfo evaluation=list.get(i);
			
			String value = evaluation.getStudentNo();
			BigDecimal bd = new BigDecimal(value);
			String studentNo = bd.toString();
			if(studentNo.indexOf(".") > 0)
				studentNo =String.valueOf(Math.round(Double.parseDouble(studentNo)));
			//获取导入学生
			StudentInfoModel student = this.studentCommonDao.queryStudentByStudentNo(studentNo);
			EvaluationInfo newEvaluation = new EvaluationInfo();
		    
			for (Dic dic : yearDic){
				if (evaluation.getYearId().equals(dic.getName())) {
					newEvaluation.setYear(dic);
					break;
				}
			}
				
			for (Dic dic : termDic){
				if (evaluation.getTermId().equals(dic.getName())) {
					newEvaluation.setTerm(dic);
					break;
				}
			}
			
			for (Dic dic : monthDic){
				if (evaluation.getMonthId().equals(dic.getName())) {
					newEvaluation.setMonth(dic);
					break;
				}
			}
			if(DataUtil.isNotNull(student)){
				newEvaluation.setStudent(student);//设置测评学生
				//学生所在班级当前测评员
			    EvaluationUser evaluationUser = this.evaluationSetService.queryEvaluationUser(student.getClassId().getId());
			    if(null != evaluationUser){
			    	newEvaluation.setAssist(evaluationUser.getAssist());//设置测评人
			    }
			    newEvaluation.setStatus(statusDic);//设置已确认状态（完成）
			    
			    List<EvaluationDetail> detaiList=new ArrayList<EvaluationDetail>();
			    for(int j=i;j<i+3;j++){
			    	EvaluationDetail detail=new EvaluationDetail();
			    	EvaluationInfo preEvaluation=list.get(j);
			    	for(Dic dic : typeDic){
						if (preEvaluation.getTypeId().equals(dic.getName())) {
							detail.setType(dic);//设置测评明细类型
							break;
						}
					}
			    	detail.setReason(preEvaluation.getReason()); //设置测评明细事由
			    	detail.setScore(preEvaluation.getScoreSum());//设置测评明细分数
			    	detaiList.add(detail);
			    }
			    
			    EvaluationDetail detail=new EvaluationDetail();//智育
			    Dic type=this.dicUtil.getDicInfo("EVALUATION_BASE_TYPE", "INTELLECT");
			    detail.setType(type);
			    detail.setReason(""); //设置测评明细事由
		    	detail.setScore("");//设置测评明细分数
		    	detaiList.add(detail);//
				
			    newEvaluation.setMoralScoreSum(StringUtils.isEmpty(detaiList.get(0).getScore())?"0":detaiList.get(0).getScore());//德育
			    newEvaluation.setCultrueScoreSum(StringUtils.isEmpty(detaiList.get(1).getScore())?"0":detaiList.get(1).getScore());//文体
			    newEvaluation.setCapacityScoreSum(StringUtils.isEmpty(detaiList.get(2).getScore())?"0":detaiList.get(2).getScore());//能力
			    
			    //插入测评基础信息表
			    this.stuEvaluationDao.saveEvaluation(newEvaluation);
			    this.flush();
				//插入测评明细表
			    for (int k=0;k<detaiList.size();k++) {
			    	detaiList.get(k).setEvaluation(newEvaluation);
			    	detaiList.get(k).setSeqNum(k);
			    	this.stuEvaluationDao.saveEvaluationDetail(detaiList.get(k));
			    	this.flush();
				}
			}
		}
		
	}
	
	/***
	 * 查询已确认综合测评记录
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryConfirmEvaluationList(int pageNum, int pageSize, EvaluationInfo evaluation){
		return this.evaluationScoreDao.queryConfirmEvaluationList(pageNum, pageSize, evaluation);
	}
	
	/***
	 * 获取当前登录人所维护的班级
	 * @param userId
	 * @return
	 */
	public List<BaseClassModel> queryEvaluationClassList(String userId){
		List<BaseClassModel> classList=new ArrayList<BaseClassModel>();
		if(this.jobTeamService.isEvaCounsellor(userId)){
			//综合测评辅导员只能查看自己所在学院的班级
			String collegeId="";
			List<BaseAcademyModel> collegeList=this.jobTeamService.getBAMByTeacherId(userId);
			if(collegeList.size()>0){
				collegeId=collegeList.get(0).getCode();
			}
			classList=this.baseDataDao.listBaseClass("", "", collegeId);
		}else{
			//测评员所负责的班级
			classList=this.evaluationScoreDao.getClassByEvaluationUser(userId);
		}
		return classList;
	}
	
	/***
	 * 生成该yearId、termId、monthId、下班级所有成员的测评记录 
	 * @param yearId
	 * @param termId
	 * @param monthId
	 * @param classId
	 */
	public void addClassEvaluation(String yearId, String termId, String monthId, String classId){
		List<StudentInfoModel> studentList=this.evaluationQueryDao.queryStudentInfoByClassId(classId);
		Dic statusDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "SAVE");
		//生成该班测测评记录 
		for (Iterator iterator = studentList.iterator(); iterator.hasNext();) {
			StudentInfoModel student = (StudentInfoModel) iterator.next();
			EvaluationInfo evaluation=this.stuEvaluationDao.getEvaluationInfo(yearId, monthId, student.getId());
			 if(!DataUtil.isNotNull(evaluation)){
				//生成记录
				evaluation=new EvaluationInfo(); 
				List<Dic> baseTypeList=this.dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
				Dic yearDic=new Dic();
				yearDic.setId(yearId);
				evaluation.setYear(yearDic);
				
				Dic termDic=new Dic();
				termDic.setId(termId);
				evaluation.setTerm(termDic);
				
				Dic monthDic=new Dic();
				monthDic.setId(monthId);
				evaluation.setMonth(monthDic); 
				
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
				
				for(int j=0;j<baseTypeList.size();j++,seqNum++){
					Dic dic = (Dic) baseTypeList.get(j);
					EvaluationDetail detail=new EvaluationDetail();
					detail.setReason("");
					detail.setEvaluation(evaluation);
					detail.setType(dic);
					detail.setSeqNum(seqNum);
					this.stuEvaluationDao.saveEvaluationDetail(detail);
				}
			 }
		}
	}
}
