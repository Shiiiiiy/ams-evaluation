package com.uws.evaluation.dao.impl;

import java.util.ArrayList;
import java.util.List;

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
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.evaluation.dao.IEvaluationScoreDao;
import com.uws.evaluation.util.EvaluationCommon;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

@Repository("evaluationScoreDao")
public class EvaluationScoreDaoImpl extends BaseDaoImpl implements IEvaluationScoreDao {
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	@Autowired
	private IStuJobTeamSetCommonService jobTeamService;
	
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	/***
	 * 查询已提交的测评记录
	 */
	public Page queryEvaluationScorePage(int pageNum, int pageSize, EvaluationInfo evaluation){
		Dic submitDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "SUBMIT");//提交状态
		Dic confirmDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "TO_CONFIRMED");//待确认状态
		String currentUserId=this.sessionUtil.getCurrentUserId();
		EvaluationCommon common=new EvaluationCommon();
		List<Object> values = new ArrayList<Object>();
		
		StringBuffer hql = new StringBuffer("from EvaluationInfoVo t where 1=1");
		
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
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getStudent()) && DataUtil.isNotNull(evaluation.getStudent().getName())){//学生姓名
			hql.append(" and t.student.name like  ? ");
			values.add("%" + HqlEscapeUtil.escape(evaluation.getStudent().getName()) + "%");
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getUserName())){//测评员
			hql.append(" and t.assist.name like ? ");
			values.add("%" + HqlEscapeUtil.escape(evaluation.getUserName()) + "%");
		}
		
		//月份
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
			hql.append(" and t.month.id  in "+common.getCondition(evaluation.getMonthId()));
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getStatus()) && (!"".equals(evaluation.getStatus().getId()))){//状态
			hql.append(" and t.status.id  = ? ");
			values.add(evaluation.getStatus().getId());
		}
		
		if(this.jobTeamService.isEvaCounsellor(currentUserId)){
			//综合测评辅导员只能查看自己班级的测评人列表
			hql.append(" and t.student.college.id in "+this.getCollegeIds(currentUserId));
		}else{
			//过滤自能看到属于自己的或者测评人员负责的班级的记录 
			hql.append(" and ( t.student.id='"+currentUserId+"' or t.evaluationUser.assist.id='"+currentUserId+"' )");
		}
		
		//过滤   只查看已提交、待确认状态
		hql.append(" and ( t.status.id='"+submitDic.getId()+"' or t.status.id='"+confirmDic.getId()+"') ");
		
		//排序条件
		hql.append(" order by t.status.id, t.student.classId.id asc ");
		
	    if(values.size()>0){
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize, values.toArray());
	    }else{
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize);
	    }
	}
	
	
	/***
	 * 查询班级下同学年、学期、月份、班级、已提交状态下的测评记录 
	 */
	public List<EvaluationInfo> getNextEvaluation(EvaluationInfo evaluation){
		Dic submitDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "SUBMIT");//提交状态
		
		StringBuffer hql=new StringBuffer("from EvaluationInfo t where t.year.id='"+evaluation.getYear().getId()+"'");
		hql.append(" and t.term.id='"+evaluation.getTerm().getId()+"'");
		hql.append(" and t.student.classId.id='"+evaluation.getStudent().getClassId().getId()+"'");
		hql.append(" and t.month.id='"+evaluation.getMonth().getId()+"'");
		hql.append(" and t.status.id='"+submitDic.getId()+"'");
		//排序条件
		hql.append(" order by t.createTime desc ");
				
		List<EvaluationInfo> list=this.query(hql.toString(), new Object[]{});
		return list;
	}
	
	/***
	 * 查询测评明细，条件：测评记录id、明细类别和名称
	 */
	public List<EvaluationDetail> getEvaluationDetail(String evaluationId, String typeId, String reason){
		
		StringBuffer hql=new StringBuffer("from EvaluationDetail t where t.evaluation.id='"+evaluationId+"'");
		hql.append(" and t.type.id='"+typeId+"'");
		hql.append(" and t.reason='"+reason+"'");
		//排序条件
		hql.append(" order by t.createTime desc ");
				
		
		List<EvaluationDetail> list=this.query(hql.toString(), new Object[]{});
		return list;
	}
	
	/***
	 * 通过导入修改测评分数
	 * @param id
	 * @param score
	 */
	public void updateEvaluationDetailScore(EvaluationDetail evaluationDetail){
		this.update(evaluationDetail);
		//this.flush();
	}
	
	/***
	 * 通过测评记录id查询明细总分
	 * @param id
	 * @return
	 */
	public List queryEvaluationSumScore(String id){
		StringBuffer sql=new StringBuffer("select t.type, sum(t.score) from HKY_EVALUATION_RECORD_DETAIL t where t.evaluation_id='"+id+"'");
		sql.append(" group by t.type ");
		
		return this.querySQL(sql.toString(), new Object[]{});
	}
	
	/***
	 * 查询基础设置的值
	 * @param baseTypeId
	 * @param scoreTypeId
	 * @return
	 */
	public String getBaseScore(String baseTypeId, String scoreTypeId){
		StringBuffer hql=new StringBuffer(" from EvaluationScore t where 1=1");
		hql.append(" and t.baseType.id='"+baseTypeId+"'"+" and t.scoreType.id='"+scoreTypeId+"'");
		
		List<EvaluationScore> list = this.query(hql.toString(), new Object[]{});
		if(list.size()>0 && list !=null){
			return list.get(0).getScore();
		}
		return "0";
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
	 * 查询已确认综合测评记录
	 * @param pageNum
	 * @param pageSize
	 * @param evaluation
	 * @return
	 */
	public Page queryConfirmEvaluationList(int pageNum, int pageSize, EvaluationInfo evaluation){
		Dic confirmDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "CONFIRMED");//已确认状态
		String currentUserId=this.sessionUtil.getCurrentUserId();
		EvaluationCommon common=new EvaluationCommon();
		List<Object> values = new ArrayList<Object>();
		
		StringBuffer hql = new StringBuffer("from EvaluationInfoVo t where 1=1");
		
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
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getStudent()) && DataUtil.isNotNull(evaluation.getStudent().getName())){//学生姓名
			hql.append(" and t.student.name like  ? ");
			values.add("%" + HqlEscapeUtil.escape(evaluation.getStudent().getName()) + "%");
		}
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getUserName())){//测评员
			hql.append(" and t.assist.name like ? ");
			values.add("%" + HqlEscapeUtil.escape(evaluation.getUserName()) + "%");
		}
		
		//月份
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getMonthId())){
			hql.append(" and t.month.id  in "+common.getCondition(evaluation.getMonthId()));
		}
		
		if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getStatus()) && (!"".equals(evaluation.getStatus().getId()))){//状态
			hql.append(" and t.status.id  = ? ");
			values.add(evaluation.getStatus().getId());
		}
		
		if(this.jobTeamService.isEvaCounsellor(currentUserId)){
			//综合测评辅导员只能查看自己班级的测评人列表
			hql.append(" and t.student.college.id in "+this.getCollegeIds(currentUserId));
		}
		
		//过滤   只查看已提交、待确认状态
		hql.append(" and t.status.id='"+confirmDic.getId()+"' ");
		
		//排序条件
		hql.append(" order by t.status.id, t.student.classId.id asc ");
		
	    if(values.size()>0){
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize, values.toArray());
	    }else{
	    	return this.pagedQuery(hql.toString(), pageNum, pageSize);
	    }
	}
	
	/***
	 * 通过测评员查询所管理的班级
	 * @param userId
	 * @return
	 */
	public List<BaseClassModel> getClassByEvaluationUser(String userId){
		StringBuffer hql=new StringBuffer("select t.classId from EvaluationUser t where 1=1");
		hql.append(" and t.assist.id='"+userId+"'");
		
		List<BaseClassModel> list = this.query(hql.toString(), new Object[]{});
		return list;
	}
}
