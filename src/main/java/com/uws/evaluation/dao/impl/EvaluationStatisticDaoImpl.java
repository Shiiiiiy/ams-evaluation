package com.uws.evaluation.dao.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.uws.common.service.ICommonRoleService;
import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.core.hibernate.dao.impl.BaseDaoImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.evaluation.dao.IEvaluationSetDao;
import com.uws.evaluation.dao.IEvaluationStatisticDao;
import com.uws.evaluation.util.EvaluationCommon;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.user.model.Org;
import com.uws.util.ProjectConstants;
import com.uws.util.ProjectSessionUtils;


/*****
 * 综合测评统计
 * @author Jiangbl
 * @date 2015-9-15
 */

@Repository("evaluationStatisticDao")
public class EvaluationStatisticDaoImpl extends BaseDaoImpl implements IEvaluationStatisticDao {

	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	@Autowired
	private IStuJobTeamSetCommonService jobTeamService;
	
	@Autowired
	private ICommonRoleService commonroleService;
	
	@Autowired
	private IEvaluationSetDao evaluationSetDao;
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	/***
	 * 测评统计
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page statisticEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation, HttpServletRequest request){
		Dic confirmDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "CONFIRMED");//已确认状态
		String orgId= ProjectSessionUtils.getCurrentTeacherOrgId(request);
		String currentUserId=this.sessionUtil.getCurrentUserId();
		EvaluationCommon common=new EvaluationCommon();
		List<Object> values = new ArrayList<Object>();
		
		StringBuffer hql = new StringBuffer(" select t.STUDENT_ID, p.NAME as collegeName, q.MAJOR_NAME, k.CLASS_NAME, s.NAME as studentName, "+
				" sum(t.MORAL_SCORE_SUM), avg(t.INTELLECT_SCORE_SUM), sum(t.CULTRUE_SCORE_SUM), sum(t.CAPACITY_SCORE_SUM), sum(t.SCORE_SUM) as scoresum, d.NAME, d.ID"+
				" from V_EVALUATION_INFO t, HKY_STUDENT_INFO s, HKY_BASE_COLLAGE p, HKY_BASE_MAJOR q, HKY_BASE_CLASS k,HKY_EVALUATION_TIME e, DIC d "+
				"  where t.STUDENT_ID = s.ID and s.COLLEGE = p.ID and s.MAJOR = q.ID and s.CLASS_ID = k.ID and t.TID=e.ID and t.YEAR_ID=d.ID");
		//学年
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getYearId())){
			hql.append(" and t.YEAR_ID = ? ");
			values.add(evaluation.getYearId());
		}

		//学院
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getCollageId())){
			hql.append(" and p.ID  = ? ");
			values.add(evaluation.getCollageId());
		}
		//专业
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMajorId())){
			hql.append(" and q.ID  = ? ");
			values.add(evaluation.getMajorId());
		}
		//班级
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.geteClassId())){
			hql.append(" and k.ID  = ? ");
			values.add(evaluation.geteClassId());
		}
		
		//月份
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
			hql.append(" and t.MONTH_ID  in "+common.getCondition(evaluation.getMonthId()));
		}
		
		//判断是否是学生处的，学生处的查全部测评信息、辅导员查看自己名下的班级的学生、班主任查看自己班级的学生的测评成绩
		if(null!=orgId && !ProjectConstants.STUDNET_OFFICE_ORG_ID.equals(orgId)){
			if(this.jobTeamService.isHeadMaster(currentUserId) && this.jobTeamService.isEvaCounsellor(currentUserId)){
				//班主任兼测评辅导员
				String classIds=this.getClassIdByHeadMasterId(currentUserId);
				hql.append(" and ( p.ID in "+this.getCollegeIds(currentUserId)+" or k.ID  in "+common.getCondition(classIds)+")");
			}else if(this.jobTeamService.isHeadMaster(currentUserId)){
				//班主任
				String classIds=this.getClassIdByHeadMasterId(currentUserId);
				hql.append(" and k.ID  in "+common.getCondition(classIds));
			}else if(this.jobTeamService.isEvaCounsellor(currentUserId)){
				//综合测评辅导员只能查看自己班级的测评人列表
				hql.append(" and p.ID in "+this.getCollegeIds(currentUserId));
			}else if(this.commonroleService.checkUserIsExist(currentUserId, "HKY_COLLEGE_DIRECTOR")){
				//二级学院学工办主任
				hql.append(" and p.ID in "+this.getCollegeDirectByTeacherId(currentUserId));
			}else if(this.jobTeamService.isTeacherCounsellor(currentUserId)){
				//教学辅导员
				String classIds=this.getClassIdByCounsellorId(currentUserId);
				hql.append(" and k.ID  in "+ classIds);
			}else{
				hql.append(" and 1 != 1");
			}
		}
		
		//过滤   只查看已确认状态
		hql.append(" and t.STATUS='"+confirmDic.getId()+"' ");
		
		//过滤没有完成确认的班级的学生的测评月份
		hql.append(" and t.ID not in (select a.id from HKY_EVALUATION_INFO a where 1=1");
		//学年
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getYearId())){
			hql.append(" and a.year_id = '"+evaluation.getYearId()+"'");
		}
		
		hql.append(" and a.student_id in (");
		hql.append( " select distinct m.id from hky_student_info m where m.class_id in (");
		hql.append(" select distinct n.class_id from hky_student_info n, HKY_EVALUATION_INFO pp where pp.status!='"+confirmDic.getId()+"' ");
		//学年
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getYearId())){
			hql.append(" and pp.year_id ='"+evaluation.getYearId()+"' ");
		}
		
		hql.append(" and  n.id=pp.student_id");
		//月份
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
			hql.append(" and pp.MONTH_ID  in "+common.getCondition(evaluation.getMonthId()));
		}
		hql.append("))");
		//月份
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
			hql.append(" and a.MONTH_ID  in "+common.getCondition(evaluation.getMonthId()));
		}
		hql.append(")");
		
		hql.append(" group by t.STUDENT_ID, p.NAME, q.MAJOR_NAME, k.CLASS_NAME, s.NAME, d.NAME, d.ID ");
		
		//排序条件
		hql.append(" order by k.CLASS_NAME, scoresum desc ");
		
		Page page=this.pagedSQLQuery(hql.toString(), pageNum, pageSize, values.toArray());
		List<Object[]> list=(List) page.getResult();
		Object[] preObjects=new Object[]{};
		Object[] maxObjects=new Object[]{};
		List maxList=new ArrayList();

		//测评分基础设置
		List<EvaluationScore> evaluationScoreList=this.evaluationSetDao.queryEvaluationScore();
		String moralBaseScore="";
		String cultureBaseScore="";
		String capacityBaseScore="";
		String moralRewardScore="";
		String cultureRewardScore="";
		String capacityRewardScore="";
		String moralWeight="";
		String cultureWeight="";
		String capacityWeight="";
		String intellectWeight="";
		
		DecimalFormat df=new DecimalFormat(".##");
		
		for (Iterator iterator = evaluationScoreList.iterator(); iterator.hasNext();) {
			EvaluationScore evaluationScore = (EvaluationScore) iterator.next();
			if(evaluationScore.getBaseType().getCode().equals("MORAL") && evaluationScore.getScoreType().getCode().equals("BASE_SCORE")){
				moralBaseScore=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("MORAL") && evaluationScore.getScoreType().getCode().equals("REWARD_SCORE")){
				moralRewardScore=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("MORAL") && evaluationScore.getScoreType().getCode().equals("WEIGHT")){
				moralWeight=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("CULTURE") && evaluationScore.getScoreType().getCode().equals("BASE_SCORE")){
				cultureBaseScore=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("CULTURE") && evaluationScore.getScoreType().getCode().equals("REWARD_SCORE")){
				cultureRewardScore=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("CULTURE") && evaluationScore.getScoreType().getCode().equals("WEIGHT")){
				cultureWeight=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("CAPACITY") && evaluationScore.getScoreType().getCode().equals("BASE_SCORE")){
				capacityBaseScore=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("CAPACITY") && evaluationScore.getScoreType().getCode().equals("REWARD_SCORE")){
				capacityRewardScore=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("CAPACITY") && evaluationScore.getScoreType().getCode().equals("WEIGHT")){
				capacityWeight=evaluationScore.getScore();
			}else if(evaluationScore.getBaseType().getCode().equals("INTELLECT") && evaluationScore.getScoreType().getCode().equals("WEIGHT")){
				intellectWeight=evaluationScore.getScore();
			}
		}
		
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Object[] objects = (Object[]) iterator.next();
			if(preObjects.length>0 && preObjects.length>0 &&  preObjects[3].toString().equals(objects[3].toString())){
			//若与前一位同一个班的最大值用上次查询的结果
				//德育分数  && (objects[5].toString().equals("0"))
				if((maxObjects[0].toString().equals("0"))){
					/*//判断最大值和个人分数是否都为0，是则比例为1
					objects[5]=df.format(Double.parseDouble(moralBaseScore)+Double.parseDouble(moralRewardScore)*1);
				}else if((maxObjects[0].toString().equals("0"))){*/
					//判断为0
					objects[5]=df.format(Double.parseDouble(moralBaseScore)+Double.parseDouble(objects[5].toString()));
				}else if((Double.parseDouble(objects[5].toString())<0 && Double.parseDouble(maxObjects[0].toString())<0)){
					//判断最大值和个人分数是都小于0或者最大值为0，是则最大分做分子，个人分做分母
					/*objects[5]=df.format(Double.parseDouble(moralBaseScore)+
							Double.parseDouble(moralRewardScore)*(Double.parseDouble(maxObjects[0].toString())/Double.parseDouble(objects[5].toString())));*/
					objects[5]=df.format(Double.parseDouble(moralBaseScore)+Double.parseDouble(objects[5].toString()));
				}else{
					//最大分大于0
					objects[5]=df.format(Double.parseDouble(moralBaseScore)+
							Double.parseDouble(moralRewardScore)*(Double.parseDouble(objects[5].toString())/Double.parseDouble(maxObjects[0].toString())));
				}
				//文体分数比 && (objects[7].toString().equals("0"))
				if((maxObjects[1].toString().equals("0"))){
					/*objects[7]=df.format(Double.parseDouble(cultureBaseScore)+Double.parseDouble(cultureRewardScore)*1);
				}else if((maxObjects[1].toString().equals("0"))){*/
						objects[7]=df.format(Double.parseDouble(cultureBaseScore)+Double.parseDouble(objects[7].toString()));
				}else if(Double.parseDouble(objects[7].toString())<0 && Double.parseDouble(maxObjects[1].toString())<0){
					/*objects[7]=df.format(Double.parseDouble(cultureBaseScore)+
							Double.parseDouble(cultureRewardScore)*(Double.parseDouble(maxObjects[1].toString())/Double.parseDouble(objects[7].toString())));*/
					objects[7]=df.format(Double.parseDouble(cultureBaseScore)+Double.parseDouble(objects[7].toString()));
				}else{
					objects[7]=df.format(Double.parseDouble(cultureBaseScore)+
							Double.parseDouble(cultureRewardScore)*(Double.parseDouble(objects[7].toString())/Double.parseDouble(maxObjects[1].toString())));
				}
				//能力分数比 && (objects[8].toString().equals("0"))
				if((maxObjects[2].toString().equals("0"))){
					/*objects[8]=df.format(Double.parseDouble(capacityBaseScore)+Double.parseDouble(capacityRewardScore)*1);
				}else if((maxObjects[2].toString().equals("0"))){*/
					objects[8]=df.format(Double.parseDouble(capacityBaseScore)+Double.parseDouble(objects[8].toString()));
				}else if(Double.parseDouble(objects[8].toString())<0 && Double.parseDouble(maxObjects[0].toString())<0){
					/*objects[8]=df.format(Double.parseDouble(capacityBaseScore)+
							Double.parseDouble(capacityRewardScore)*(Double.parseDouble(maxObjects[2].toString())/Double.parseDouble(objects[8].toString())));*/
					objects[8]=df.format(Double.parseDouble(capacityBaseScore)+Double.parseDouble(objects[8].toString()));
				}else{
					objects[8]=df.format(Double.parseDouble(capacityBaseScore)+
							Double.parseDouble(capacityRewardScore)*(Double.parseDouble(objects[8].toString())/Double.parseDouble(maxObjects[2].toString())));
				}
			}else{
				String studentNum=objects[0].toString();
				String yearId=objects[11].toString();
				//获取查询的当前学年、测评月份、班级的德育、文体、能力最高分
				StringBuffer maxValueHql = 
						new StringBuffer("select max(sum(k.moral_score_sum)) as moral_score_sum,"+
										" max(sum(k.cultrue_score_sum)) as cultrue_score_sum,"+
										" max(sum(k.capacity_score_sum)) as capacity_score_sum"+
										" from (select t.month_id, t.student_id as student_id, t.year_id,"+
										" sum(t.moral_score_sum) as moral_score_sum,"+
										" sum(t.cultrue_score_sum) as cultrue_score_sum,"+
										" sum(t.capacity_score_sum) as capacity_score_sum"+
										" from hky_evaluation_info t left join HKY_STUDENT_INFO s on t.student_id=s.id "+
										" left join HKY_BASE_CLASS k on s.class_id=k.id "+
										" where  k.id in(select c.class_id from HKY_STUDENT_INFO c where c.id='"+studentNum+"')"+
										" group by t.student_id, t.year_id, t.month_id) k where 1=1 ");
				//月份
				if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
					maxValueHql.append(" and k.MONTH_ID  in "+common.getCondition(evaluation.getMonthId()));
				}
				maxValueHql.append(" group by k.student_id, k.year_id ");
				
				maxList=this.querySQL(maxValueHql.toString(), new Object[]{});
				if(maxList.size()>0){
					maxObjects=(Object[]) maxList.get(0);
					//德育分数   && (objects[5].toString().equals("0"))
					if((maxObjects[0].toString().equals("0"))){
						//判断最大值和个人分数是否都为0，是则比例为1
						/*objects[5]=df.format(Double.parseDouble(moralBaseScore)+Double.parseDouble(moralRewardScore)*1);
					}else if((maxObjects[0].toString().equals("0"))){
						//判断最大值为0，分数为奖惩分
*/						objects[5]=df.format(Double.parseDouble(moralBaseScore)+Double.parseDouble(objects[5].toString()));
					}else if((Double.parseDouble(objects[5].toString())<0 && Double.parseDouble(maxObjects[0].toString())<0)){
					//判断最大值和个人分数是都小于0，是则最大分做分子，个人分做分母
					/*objects[5]=df.format(Double.parseDouble(moralBaseScore)+
							Double.parseDouble(moralRewardScore)*(Double.parseDouble(maxObjects[0].toString())/Double.parseDouble(objects[5].toString())));*/
						objects[5]=df.format(Double.parseDouble(moralBaseScore)+Double.parseDouble(objects[5].toString()));
					}else{
						//最大分大于0
						objects[5]=df.format(Double.parseDouble(moralBaseScore)+
								Double.parseDouble(moralRewardScore)*(Double.parseDouble(objects[5].toString())/Double.parseDouble(maxObjects[0].toString())));
					}
					//文体分数比 && (objects[7].toString().equals("0"))
					if((maxObjects[1].toString().equals("0"))){
						/*objects[7]=df.format(Double.parseDouble(cultureBaseScore)+Double.parseDouble(cultureRewardScore)*1);
					}else if(maxObjects[1].toString().equals("0")){*/
						objects[7]=df.format(Double.parseDouble(cultureBaseScore)+Double.parseDouble(objects[7].toString()));
					}else if(Double.parseDouble(objects[7].toString())<0 && Double.parseDouble(maxObjects[1].toString())<0){
						/*objects[7]=df.format(Double.parseDouble(cultureBaseScore)+
							Double.parseDouble(cultureRewardScore)*(Double.parseDouble(maxObjects[1].toString())/Double.parseDouble(objects[7].toString())));*/
						objects[7]=df.format(Double.parseDouble(cultureBaseScore)+Double.parseDouble(objects[7].toString()));
					}else{
						objects[7]=df.format(Double.parseDouble(cultureBaseScore)+
								Double.parseDouble(cultureRewardScore)*(Double.parseDouble(objects[7].toString())/Double.parseDouble(maxObjects[1].toString())));
					}
					//能力分数比 && (objects[8].toString().equals("0"))
					if((maxObjects[2].toString().equals("0"))){
					/*	objects[8]=df.format(Double.parseDouble(capacityBaseScore)+Double.parseDouble(capacityRewardScore)*1);
					}else if((maxObjects[2].toString().equals("0"))){*/
						objects[8]=df.format(Double.parseDouble(capacityBaseScore)+Double.parseDouble(objects[8].toString()));
					}else if(Double.parseDouble(objects[8].toString())<0 && Double.parseDouble(maxObjects[2].toString())<0){
						/*objects[8]=df.format(Double.parseDouble(capacityBaseScore)+
							Double.parseDouble(capacityRewardScore)*(Double.parseDouble(maxObjects[2].toString())/Double.parseDouble(objects[8].toString())));*/
						objects[8]=df.format(Double.parseDouble(capacityBaseScore)+Double.parseDouble(objects[8].toString()));
					}else{
						objects[8]=df.format(Double.parseDouble(capacityBaseScore)+
								Double.parseDouble(capacityRewardScore)*(Double.parseDouble(objects[8].toString())/Double.parseDouble(maxObjects[2].toString())));
					}
				}
			}
			
			//算总分
			objects[9]=df.format(Double.parseDouble(objects[5].toString())*Double.parseDouble(moralWeight)
								+Double.parseDouble((objects[6]!=null?objects[6]:"0").toString())*Double.parseDouble(intellectWeight)
								+Double.parseDouble(objects[7].toString())*Double.parseDouble(cultureWeight)
								+Double.parseDouble(objects[8].toString())*Double.parseDouble(capacityWeight));
			
			preObjects=objects;//
		}
		
		return page;
	}
	
	/***
	 * 获取班主任下的所有班级id
	 * @param id
	 * @return
	 */
	private String getClassIdByHeadMasterId(String id){
		String ids="";
		List<BaseClassModel> list=this.jobTeamService.getHeadteacherClass(id);
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			BaseClassModel baseClassModel = (BaseClassModel) iterator.next();
			ids+=baseClassModel.getId()+",";
		}
		return ids;
	}
	
	/****
	 * 调用学工模块通过当前测评辅导员id获取所管理的学院
	 * @param Ids
	 * @return
	 */
	public String getCollegeIds(String userId) {
		List<BaseAcademyModel> list=this.jobTeamService.getBAMByTeacherId(userId);
		StringBuffer sbff = new StringBuffer();
		sbff.append(" (");
		for (int i = 0; i < list.size(); i++) {
			if(list.size()-1==i){
				sbff.append("'"+list.get(i).getId()+"'");
			}else{
				sbff.append("'"+list.get(i).getId()+"'").append(",");
			}
		} 
		sbff.append(")");

		return sbff.toString();
	}
	
	/***
	 *  获取教师所属的学院
	 * @param baseTeacherId
	 * @param teacherTypeId
	 * @return
	 */
	public String getCollegeDirectByTeacherId(String teacherId) {
		String hql = "select t.org from BaseTeacherModel t where t.id = ? ";
		List<Org> list=(List<Org>)this.query(hql, new String[]{teacherId});
		StringBuffer sbff = new StringBuffer();
		sbff.append(" (");
		for (int i = 0; i < list.size(); i++) {
			if(list.size()-1==i){
				sbff.append("'"+list.get(i).getCode()+"'");
			}else{
				sbff.append("'"+list.get(i).getCode()+"'").append(",");
			}
		} 
		sbff.append(")");

		return sbff.toString();
	}
	
	/***
	 * 获取教学辅导员所带的班级
	 * @param userId
	 * @return
	 */
	private String getClassIdByCounsellorId(String userId) {
		List<BaseClassModel> list=this.jobTeamService.queryBaseClassModelByTCId(userId);
		StringBuffer sbff = new StringBuffer();
		sbff.append(" (");
		for (int i = 0; i < list.size(); i++) {
			if(list.size()-1==i){
				sbff.append("'"+list.get(i).getId()+"'");
			}else{
				sbff.append("'"+list.get(i).getId()+"'").append(",");
			}
		} 
		sbff.append(")");

		return sbff.toString();
	}
	
}
