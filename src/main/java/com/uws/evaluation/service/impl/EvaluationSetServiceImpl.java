package com.uws.evaluation.service.impl;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.uws.common.dao.ICommonRoleDao;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTerm;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.evaluation.EvaluationUser;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.dao.IEvaluationSetDao;
import com.uws.evaluation.service.IEvaluationSetService;
import com.uws.evaluation.service.IStuEvaluationService;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.user.model.User;
import com.uws.util.ProjectConstants;

/**
 * @Description 综合测评基础设置Service
 * @author Jiangbl
 * @date 2015-8-13
 */

@Service("evaluationSetService")
public class EvaluationSetServiceImpl implements IEvaluationSetService{
	
	@Autowired
	private IEvaluationSetDao evaluationSetDao;
	
	@Autowired
	private ICommonRoleDao commonRoleDao;
	
	@Autowired
	private IStuEvaluationService stuEvaluationservice;
	
	private DicUtil dicUtil = DicFactory.getDicUtil();
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	/**
	 * 查询基础分
	 */
	public List<EvaluationScore> queryEvaluationScore(){
		return this.evaluationSetDao.queryEvaluationScore();
	}
	
	/**
	 * 保存基础分
	 */
	public void saveEvaluationScore(EvaluationScore evaluationScore){
		this.evaluationSetDao.saveEvaluationScore(evaluationScore);
	}
	
	/**
	 * 更新基础分
	 */
	public void updateEvaluationScore(EvaluationScore evaluationScore){
		this.evaluationSetDao.updateEvaluationScore(evaluationScore);
	}
	
	public EvaluationScore getEvaluationScoreById(String id){
		return this.evaluationSetDao.getEvaluationScoreById(id);
	}
	
	/**
	 * 综合测评时间设置查询
	 */
	public Page queryEvaluationTimePage(int pageNum, int pageSize, EvaluationTime evaluationTime, String currentUserId){
		return this.evaluationSetDao.queryEvaluationTimePage(pageNum, pageSize, evaluationTime, currentUserId);
	}
	
	/**
	 * 保存综合测评时间设置
	 */
	public void saveEvaluationTime(EvaluationTime evaluationTime){
		//学院
		BaseAcademyModel college=new BaseAcademyModel();
		college.setId(evaluationTime.getCollegeId());
		evaluationTime.setCollege(college);
		
		//辅导员
		User instructor=new User();
		instructor.setId(evaluationTime.getInstructorId());
		evaluationTime.setInstructor(instructor);
		
		//月份
		Dic month=new Dic();
		month.setId(evaluationTime.getMonthId());
		evaluationTime.setMonth(month);
		
		//操作人
		User creator=new User(this.sessionUtil.getCurrentUserId());//当前登录人
		evaluationTime.setCreator(creator);
		
		evaluationTime.setStatus(this.dicUtil.getStatusNormal());//设置状态
		
		this.evaluationSetDao.saveEvaluationTime(evaluationTime);
	}
	
	/**
	 * 更新综合测评时间设置
	 */
	public void updateEvaluationTime(EvaluationTime evaluationTime){
		//学院
		BaseAcademyModel college=new BaseAcademyModel();
		college.setId(evaluationTime.getCollegeId());
		evaluationTime.setCollege(college);
		
		//辅导员
		User instructor=new User();
		instructor.setId(evaluationTime.getInstructorId());
		evaluationTime.setInstructor(instructor);
		
		//月份
		Dic month=new Dic();
		month.setId(evaluationTime.getMonthId());
		evaluationTime.setMonth(month);
		
		//操作人
		User creator=new User(this.sessionUtil.getCurrentUserId());//当前登录人
		evaluationTime.setCreator(creator);
		
		EvaluationTime newEvaluationTime=this.getEvaluationTimeById(evaluationTime.getId());
		BeanUtils.copyProperties(evaluationTime,newEvaluationTime,new String[]{"createTime", "college", "month", "status"});
		
		this.evaluationSetDao.updateEvaluationTime(newEvaluationTime);
	}
	
	/**
	 * 通过id获取测评时间设置
	 */
	public EvaluationTime getEvaluationTimeById(String id){
		return this.evaluationSetDao.getEvaluationTimeById(id);
	}
	
	/**
	 * 逻辑删除综合测评时间
	 */
	public void deleteEvaluationTimeById(String id){
		EvaluationTime evaluationTime=this.getEvaluationTimeById(id);
		evaluationTime.setStatus(this.dicUtil.getStatusDeleted());//设置删除状态
		this.evaluationSetDao.updateEvaluationTime(evaluationTime);
		//this.evaluationSetDao.deleteEvaluationTimeById(id);
	}
	
	/**
	 * 根据学院、月份判断是否存在测评时间设置
	 */
	public Boolean getEvaluationTime(String collegeId, String monthId, String id){
		return this.evaluationSetDao.getEvaluationTime(collegeId, monthId, id);
	}
	
	/**
	 * 查询班级测评人列表
	 */
	public Page queryClassEvaluationUserPage(int pageNum, int pageSize, EvaluationUser evaluationUser){
		return this.evaluationSetDao.queryClassEvaluationUserPage(pageNum, pageSize, evaluationUser);
	}
	
	/**
	 * 查询班级下的测评员
	 */
	public EvaluationUser queryEvaluationUser(String classId){
		return this.evaluationSetDao.queryEvaluationUser(classId);
	}
	
	/**
	 * 保存测评员
	 */
	public void saveEvaluationUser(String userId, String classId){
		EvaluationUser user=new EvaluationUser();
		//班级
		BaseClassModel eClass=new BaseClassModel();
		eClass.setId(classId);
		user.setClassId(eClass);
		//学生
		User assist = new User();
		assist.setId(userId);
		user.setAssist(assist);
		
		User creator=new User(this.sessionUtil.getCurrentUserId());//当前登录人
		user.setCreator(creator);
		this.evaluationSetDao.saveEvaluationUser(user);
		commonRoleDao.saveUserRole(userId, ProjectConstants.EVALUATION_ROLE_NAME);//添加测评员角色
	}
	
	/**
	 * 更新测评员
	 */
	public void updateEvaluationUser(EvaluationUser user, String userId){
		List<EvaluationUser> list=this.evaluationSetDao.getEvaluationUserListByUserId(user.getAssist().getId());
		
		/**
		 * 之前 新增评测员人角色， 没有校验先评测员是否 已经拥有评测员角色 ，导致出现 重复的  用户角色信息  ，后面更新评人员角色  是用queryUnique查询的 ，
		 * 有重复数据会报错，导致下面更改班级评测员操作都没执行。                 
		 */
		try{
			if(list.size()>1){//原测评员担任多个班的测评员修改后还是测评员身份，添加新人测评人角色
				commonRoleDao.saveUserRole(userId, ProjectConstants.EVALUATION_ROLE_NAME);
			}else{//原测评员一个班的测评员，换人后不再是测评员角色，添加新人测评人角色
				commonRoleDao.updateUserRole(user.getAssist().getId(), userId, ProjectConstants.EVALUATION_ROLE_NAME);
				commonRoleDao.deleteUserRole(user.getAssist().getId(), ProjectConstants.EVALUATION_ROLE_NAME);
			}
		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("评测员 【 角色信息 】更改失败....................");
		}
		 
		//测评员：
		User assist  = new User();
		assist.setId(userId);
		user.setAssist(assist);
		
		//操作人：当前登录人
		User creator=new User(this.sessionUtil.getCurrentUserId());
		user.setCreator(creator);
		this.evaluationSetDao.updateEvaluationUser(user);
	}
	
	/***
	 * 查询测评基础学期设置
	 * @return
	 */
	public List<EvaluationTerm> queryEvaluationTerm(){
		return this.evaluationSetDao.queryEvaluationTerm();
	}

	/***
	 * 保存测评学期设置
	 * 先删除旧数据在保存新数据
	 * @param request
	 */
	public void saveEvaluationTerm(HttpServletRequest request){
		//delete旧数据
		this.evaluationSetDao.deleteEvaluationTerm();
		
		//save新数据
		List<Dic> termList = this.dicUtil.getDicInfoList("TERM");	//学期
		for (Dic termDic : termList) {
			String termId=termDic.getId();
			String[] monthArray=request.getParameterValues(termId);
			if(DataUtil.isNotNull(monthArray)){
				for(int i=0;i<monthArray.length;i++){
					EvaluationTerm evaluationTerm=new EvaluationTerm();
					evaluationTerm.setTerm(termDic);
					Dic monthDic=this.stuEvaluationservice.getDicById(monthArray[i]);
					evaluationTerm.setMonth(monthDic);
					
					this.evaluationSetDao.saveEvaluationTerm(evaluationTerm);
				}
			}
		}
	}
}
