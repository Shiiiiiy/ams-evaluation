package com.uws.evaluation.dao.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.uws.core.hibernate.dao.impl.BaseDaoImpl;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.core.util.HqlEscapeUtil;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.dao.IStuEvaluationDao;
import com.uws.evaluation.util.EvaluationCommon;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

@Repository("stuEvaluationDao")
public class StuEvaluationDaoImpl extends BaseDaoImpl implements IStuEvaluationDao {
	
	private DicUtil dicUtil = DicFactory.getDicUtil();
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	public Page queryEvaluationPage(int pageNum, int pageSize, EvaluationInfo evaluation){
		List<Object> values = new ArrayList<Object>();
		StringBuffer hql = new StringBuffer("from EvaluationInfoVo t where 1=1");
		String userId=this.sessionUtil.getCurrentUserId();
		EvaluationCommon common=new EvaluationCommon();
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getYearId())){//学年
			hql.append(" and t.year.id = ? ");
			values.add(evaluation.getYearId());
		}

		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getTermId())){//学期
			hql.append(" and t.term.id = ? ");
			values.add(evaluation.getTermId());
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getCollageId())){//学院
			hql.append(" and t.student.college.id  = ? ");
			values.add(evaluation.getCollageId());
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMajorId())){//专业
			hql.append(" and t.student.major.id  = ? ");
			values.add(evaluation.getMajorId());
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.geteClassId())){//班级
			hql.append(" and t.student.classId.id  = ? ");
			values.add(evaluation.geteClassId());
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getUserName())){//测评员
			hql.append(" and t.assist.name like ? ");
			values.add("%" + HqlEscapeUtil.escape(evaluation.getUserName()) + "%");
		}
		
		//月份
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
			hql.append(" and t.month.id  in "+common.getCondition(evaluation.getMonthId()));
		}
		
		//过滤自能看到属于自己的或者测评人员、辅导员负责的记录 userId
		//hql.append(" and  t.student.id='"+userId+"' or t.evaluationUser.assist.id='"+userId+"' or t.evaluationTime.instructor.id='"+userId+"' ");
		hql.append(" and  t.student.id='"+userId+"' ");
		
		//排序条件
		hql.append(" order by t.createTime desc ");
		
	    if(values.size()>0){
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize, values.toArray());
	    }else{
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize);
	    }
	}
	
	/**
	 * 获取学院collegeId当前可添加的测评月份
	 */
	public List<EvaluationTime> getEvaluationTimeByCollegeId(String collegeId){
		Dic statusDic=this.dicUtil.getStatusNormal();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
		StringBuffer hql = new StringBuffer(" from EvaluationTime t where t.college.id = ? ");
		hql.append(" and t.status.id= ? ");
		hql.append(" and to_date('"+df.format(new Date())+"','yyyy-mm-dd') BETWEEN t.addStartTime AND t.addEndTime ");
		hql.append(" order by t.month.seqNum ");
		List<EvaluationTime> list=this.query(hql.toString(), new Object[]{collegeId, statusDic.getId()});
		return list;
	}
	
	/**
	 * 获取字典
	 */
	public Dic getDicById(String id){
		if(DataUtil.isNotNull(id)){
			return (Dic)this.get(Dic.class, id);
		}
		return null;
	}
	
	/***
	 * 获取个人测评信息
	 */
	public EvaluationInfo getEvaluationInfo(String year, String month, String user){
		
		StringBuffer hql=new StringBuffer(" from EvaluationInfo t where t.year.id = ? and t.month.id = ? and t.student.id = ? ");
		List<EvaluationInfo> list=this.query(hql.toString(), new Object[]{year, month, user});
		EvaluationInfo evaluation=new EvaluationInfo();
		if(list.size()>0){
			evaluation=list.get(0);
		}else{
			evaluation=null;
		}
		return evaluation;
	}
	
	/**
	 * 获取个人测评信息明细
	 */
	public List<EvaluationDetail> getEvaluationDetailListByEvaluationId(String id){
		StringBuffer hql=new StringBuffer(" from EvaluationDetail t where t.evaluation.id = ? ");
		hql.append(" order by t.seqNum asc");
		List<EvaluationDetail> list=this.query(hql.toString(), new Object[]{id});
		
		return list;
	}
	
	/**
	 * 保存测评信息
	 */
	public void saveEvaluation(EvaluationInfo evaluation){
		this.save(evaluation);
	}
	
	/**
	 * 更新测评信息
	 */
	public void updateEvaluation(EvaluationInfo evaluation){
		this.update(evaluation);
	}
	
	/**
	 * 通过id获取EvaluationInfo
	 */
	public EvaluationInfo getEvaluationInfoById(String id){
		StringBuffer hql = new StringBuffer(" from EvaluationInfo t where t.id = ? ");
		return (EvaluationInfo)this.queryUnique(hql.toString(), id);
	}
	
	/***
	 * 保存测评明细
	 */
	public void saveEvaluationDetail(EvaluationDetail detail){
		this.save(detail);
	}
	
	/**
	 * 更新测评明细
	 */
	public void updateEvaluationDetail(EvaluationDetail detail){
		this.update(detail);
	}
	
	/***
	 * 查询单个测评明细
	 */
	public EvaluationDetail getEvaluationDetailById(String id){
		StringBuffer hql = new StringBuffer(" from EvaluationDetail t where t.id = ? ");
		return (EvaluationDetail)this.queryUnique(hql.toString(), id);
	}
	
	/***
	 * 删除明细
	 */
	public void deleteEvaluationDetailByIds(String id, String[] detailIds){
		if(StringUtils.hasText(id)){
	    	Map<String,Object> map = new HashMap<String,Object>();
	    	StringBuffer hql = new StringBuffer(" delete from EvaluationDetail t where  t.evaluation.id = '");
	    	hql.append(id);
	    	hql.append("' ");
	    	if(!ArrayUtils.isEmpty(detailIds)){
	    		hql.append(" and t.id not in (:detailIds)");
	    		map.put("detailIds", detailIds);
	    	}
	    	this.executeHql(hql.toString(), map);
	    }
	}
	
	public void deleteEvaluationById(String id){
		//删除测评信息
		StringBuffer hql = new StringBuffer(" delete from EvaluationInfo t where t.id = ? ");
		this.executeHql(hql.toString(), new Object[]{id});
		
		//删除测评明细
		StringBuffer hqlSub = new StringBuffer(" delete from EvaluationDetail t where  t.evaluation.id = ? ");
		this.executeHql(hqlSub.toString(), new Object[]{id});
	}
	
}
