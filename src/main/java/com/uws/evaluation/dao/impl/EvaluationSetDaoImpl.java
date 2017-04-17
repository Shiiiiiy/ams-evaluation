package com.uws.evaluation.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.core.hibernate.dao.impl.BaseDaoImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.core.util.HqlEscapeUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTerm;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.evaluation.EvaluationUser;
import com.uws.evaluation.dao.IEvaluationSetDao;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

/**
 * @Description 综合测评基础设置Dao
 * @author Jiangbl
 * @date 2015-8-13
 */

@Repository("evaluationSetDao")
public class EvaluationSetDaoImpl extends BaseDaoImpl implements IEvaluationSetDao{
	@Autowired
	private IStuJobTeamSetCommonService jobTeamService;
	
	private DicUtil dicUtil = DicFactory.getDicUtil();
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
		
	/**
	 * 查询基础分
	 */
	public List<EvaluationScore> queryEvaluationScore(){
		
		List<EvaluationScore> evaluationList=this.query("from EvaluationScore ec", new Object[]{});
		return evaluationList;
	}
	
	/**
	 * 保存基础分
	 */
	public void saveEvaluationScore(EvaluationScore evaluationScore){
		this.save(evaluationScore);
	}
	
	/**
	 * 更新基础分
	 */
	public void updateEvaluationScore(EvaluationScore evaluationScore){
		this.update(evaluationScore);
	}
	
	/**
	 * 通过id查询基础分
	 */
	public EvaluationScore getEvaluationScoreById(String id){
		StringBuffer hql = new StringBuffer(" from EvaluationScore et where et.id = ? ");
		return (EvaluationScore)this.queryUnique(hql.toString(), id);
	}
	
	/**
	 * 综合测评时间设置查询
	 */
	public Page queryEvaluationTimePage(int pageNum, int pageSize, EvaluationTime evaluationTime, String currentUserId){
		Dic statusDic=this.dicUtil.getStatusNormal();
		List<Object> values = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer(" from EvaluationTime et where et.status.id='" + statusDic.getId() + "'");

		if(DataUtil.isNotNull(evaluationTime) && DataUtil.isNotNull(evaluationTime.getCollegeId())){//学院
			hql.append(" and et.college.id = ? ");
			values.add(evaluationTime.getCollegeId());
		}
		
		if(DataUtil.isNotNull(evaluationTime) && DataUtil.isNotNull(evaluationTime.getMonthId())){//月份
			hql.append(" and et.month.id = ? ");
			values.add(evaluationTime.getMonthId());
		}
		if(this.jobTeamService.isEvaCounsellor(currentUserId)){
			//综合测评辅导员只能查看自己所在的学院
			hql.append(" and et.college.id in "+getCollegeIds(currentUserId));
		}
		
		//排序条件
		hql.append(" order by et.month.seqNum , createTime desc ");
		
	    if(values.size()>0){
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize, values.toArray());
	    }else{
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize);
	    }
	}
	
	/**
	 * 保存综合测评时间设置
	 */
	public void saveEvaluationTime(EvaluationTime evaluationTime){
		this.save(evaluationTime);
	}
	
	/**
	 * 更新综合测评时间设置
	 */
	public void updateEvaluationTime(EvaluationTime evaluationTime){
		this.update(evaluationTime);
	}
	
	/**
	 * 根据id查询综合测评时间设置
	 */
	public EvaluationTime getEvaluationTimeById(String id){
		StringBuffer hql = new StringBuffer(" from EvaluationTime et where et.id = ? ");
		return (EvaluationTime)this.queryUnique(hql.toString(), id);
	}
	
	/**
	 * 删除综合测评时间设置
	 */
	public void deleteEvaluationTimeById(String id){
		StringBuffer hql = new StringBuffer(" delete from EvaluationTime et where et.id = ? ");
		this.executeHql(hql.toString(), new Object[]{id});
	}
	
	/**
	 * 判断综合测评时间设置重复
	 */
	public Boolean getEvaluationTime(String collegeId, String monthId, String id){
		Dic statusDic=this.dicUtil.getStatusNormal();
		
		List<String> values=new ArrayList<String>();
		StringBuffer hql = new StringBuffer(" from EvaluationTime et where et.status.id='" + statusDic.getId() + "'");
		if(StringUtils.isNotEmpty(collegeId)){
			hql.append(" and et.college.id = ? ");
			values.add(collegeId);
		}
		if(StringUtils.isNotEmpty(monthId)){
			hql.append(" and et.month.id = ? ");
			values.add(monthId);
		}
		List<EvaluationTime> list=this.query(hql.toString(), values.toArray());
		if(list.size()>0){
			if(id.equals("") || !(id.equals(list.get(0).getId()))){
				return true;	
			}
		}
		
		return false;
	}
	
	/**
	 * 查询各班级的测评员
	 */
	public Page queryClassEvaluationUserPage(int pageNum, int pageSize, EvaluationUser evaluationUser){
		List<Object> values = new ArrayList<Object>();
		String currentUserId=this.sessionUtil.getCurrentUserId();
		
		StringBuffer hql = new StringBuffer("select e,b from EvaluationUser b right outer join b.classId e where 1=1 ");
		
		if (DataUtil.isNotNull(evaluationUser.getCollageId())) {//学院
			hql.append(" and e.major.collage.id = ? ");
			values.add(evaluationUser.getCollageId());
		}
		
		if (DataUtil.isNotNull(evaluationUser.getMajorId())) {// 专业
			hql.append(" and e.major.id = ? ");
			values.add(evaluationUser.getMajorId());
		}
		
		if (DataUtil.isNotNull(evaluationUser.geteClassId())) {// 班级
			hql.append(" and e.id = ? ");
			values.add(evaluationUser.geteClassId());
		}
		
		if (DataUtil.isNotNull(evaluationUser.getUserName())) {// 测评员   模糊查询
			hql.append(" and b.assist.name like ? ");
			values.add("%" + HqlEscapeUtil.escape(evaluationUser.getUserName()) + "%");
		}
		
		if(this.jobTeamService.isEvaCounsellor(currentUserId)){
			//综合测评辅导员只能查看自己班级的测评人列表
			hql.append(" and e.major.collage.id in "+getCollegeIds(currentUserId));
		}
		 
		hql.append(" order by e.major.collage.id, e.major.id, e.id desc ");
		
		if (values.size() == 0)
			return this.pagedQuery(hql.toString(), pageNum, pageSize);
		else
			return this.pagedQuery(hql.toString(), pageNum, pageSize,values.toArray());
	}
	
	/**
	 * 查询班级下的测评员
	 */
	public EvaluationUser queryEvaluationUser(String classId){
		
		EvaluationUser evaluationUser=new EvaluationUser();
		
		List<EvaluationUser> list=this.query("from EvaluationUser eu where eu.classId.id = ? ", new Object[]{classId});
		if(list.size()>0){
			evaluationUser=list.get(0);
		}else{
			evaluationUser=null;
		}
		return evaluationUser;
	}
	
	/**
	 * 保存测评员
	 */
	public void saveEvaluationUser(EvaluationUser user){
		this.save(user);
	}
	
	/**
	 * 更新测评员
	 */
	public void updateEvaluationUser(EvaluationUser user){
		this.update(user);
	}
	
	/***
	 * 通过userId查询该用户所管测评的所有班级
	 * @param userId
	 */
	public List<EvaluationUser> getEvaluationUserListByUserId(String userId){
		List<EvaluationUser> list=this.query("from EvaluationUser t where t.assist.id = ? ", new Object[]{userId});
		return list;
	}
	
	/***
	 * 查询测评基础学期设置
	 * @return
	 */
	public List<EvaluationTerm> queryEvaluationTerm(){
		List<EvaluationTerm> evaluationTermList=this.query("from EvaluationTerm t", new Object[]{});
		return evaluationTermList;
	}
	
	/***
	 * 删除测评学期设置
	 */
	public void deleteEvaluationTerm(){
		StringBuffer hql = new StringBuffer(" delete from EvaluationTerm t ");
		this.executeHql(hql.toString(), new Object[]{});
	}
	
	/***
	 * 保存学期设置
	 * @param evaluationTerm
	 */
	public void saveEvaluationTerm(EvaluationTerm evaluationTerm){
		this.save(evaluationTerm);
	}

	/****
	 * 调用学工模块通过当前测评辅导员id获取所管理的学院
	 * @param Ids
	 * @return
	 */
	private String getCollegeIds(String userId) {
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
}
