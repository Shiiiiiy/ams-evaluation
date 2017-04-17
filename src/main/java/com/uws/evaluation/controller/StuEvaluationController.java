package com.uws.evaluation.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.uws.common.service.IBaseDataService;
import com.uws.common.service.IStudentCommonService;
import com.uws.common.util.SchoolYearUtil;
import com.uws.comp.service.ICompService;
import com.uws.core.base.BaseController;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.base.BaseMajorModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.orientation.StudentInfoModel;
import com.uws.evaluation.service.IStuEvaluationService;
import com.uws.log.Logger;
import com.uws.log.LoggerFactory;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.util.ProjectSessionUtils;

@Controller
public class StuEvaluationController extends BaseController{
	//log
	private Logger log=new LoggerFactory(StuEvaluationController.class);
	
	@Autowired
	private IStuEvaluationService stuEvaluationservice;
	
	@Autowired
	private IBaseDataService baseDataService;
	
	@Autowired
	private ICompService compService;
	
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	@Autowired
	private IStudentCommonService studentCommonService;
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	/***
	 * 查询个人测评信息列表
	 * @param model
	 * @param request
	 * @param evaluation
	 * @return
	 */
	@RequestMapping("/evaluation/evaluation/opt-query/queryEvaluationList")
	public String queryEvaluationPage(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation){
		log.info("查询综合测评!");
		List<BaseAcademyModel> collageList = this.baseDataService.listBaseAcademy();//学院列表
		List<BaseMajorModel> majorList = null;	//专业列表
		List<BaseClassModel> classList = null;	//班级列表
		List<Dic> schoolYearList = this.dicUtil.getDicInfoList("YEAR");	//学年
		List<Dic> termList = this.dicUtil.getDicInfoList("TERM");	//学期
		List<Dic> monthList = this.dicUtil.getDicInfoList("MONTH");	//测评月份
		if (null != evaluation) {
			if (null != evaluation.getCollageId()) {// 下拉列表 专业
				majorList = this.compService.queryMajorByCollage(evaluation.getCollageId());
				log.debug("若已经选择学院，则查询学院下的专业信息.");
			}
			
			if (null != evaluation.getMajorId()) {// 下拉列表 班级
				classList = this.compService.queryClassByMajor(evaluation.getMajorId());
				log.debug("若已经选择专业，则查询专业下的班级信息.");
			}
		}
		//默认是当期、学年
		Dic termDic = SchoolYearUtil.getCurrentTermDic();
		Dic yearDic = SchoolYearUtil.getYearDic();
		if(null == evaluation.getYearId()){//默认当前学年
			evaluation.setYearId(yearDic.getId());
		}
		
		Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
		Page page = this.stuEvaluationservice.queryEvaluationPage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation);
		
		String userId=this.sessionUtil.getCurrentUserId();//当前登录人id
		List<EvaluationTime> evaluationTimeList=this.stuEvaluationservice.getEvaluationTimeByUserId(userId);//获取当前时间可以添加测评的测评月份
		
		model.addAttribute("termDic", termDic);
		model.addAttribute("yearDic", yearDic);
		model.addAttribute("collageList", collageList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("schoolYearList", schoolYearList);
		model.addAttribute("termList", termList);
		model.addAttribute("monthList", monthList);
		model.addAttribute("evaluationTimeList", evaluationTimeList);
		model.addAttribute("page", page);
		model.addAttribute("currentDate", new Date());

		return "/evaluation/stuEvaluation/stuEvaluationList";
	}
	
	/***
	 * 编辑测评信息
	 * @param model
	 * @param yearId
	 * @param termId
	 * @param monthId
	 * @return
	 */
	@RequestMapping({"/evaluation/evaluation/opt-add/getEvaluationDetail","evaluation/evaluation/opt-update/editEvaluationInfo"})
	public String getEvaluationDetail(ModelMap model, HttpServletRequest request, String yearId, String termId, String monthId, String id){
		String userId=this.sessionUtil.getCurrentUserId();//当前登录人id
		
		EvaluationInfo evaluation=new EvaluationInfo();
		if(DataUtil.isNotNull(id)){//列表页面 编辑
			evaluation=this.stuEvaluationservice.getEvaluationInfoById(id);
			if(DataUtil.isNotNull(evaluation)){
				yearId=evaluation.getYear().getId();
				termId=evaluation.getTerm().getId();
				monthId=evaluation.getMonth().getId();
			}
		}else{//页面添加   若存在该月份记录则为修改，否则 添加
			evaluation=this.stuEvaluationservice.getEvaluationInfo(yearId, monthId, userId);
			if(evaluation == null){
				evaluation=new EvaluationInfo();
			}
			/*if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getStatus()) && "TO_CONFIRMED".equals(evaluation.getStatus().getCode())){
				//判断当前状态是否待确认  若是则跳转到确认页面
				return "redirect:/evaluation/evaluation/opt-query/queryEvaluationDetail.do?id="+evaluation.getId()+"&command=CONFIRM";
			}else if(DataUtil.isNotNull(evaluation) && DataUtil.isNotNull(evaluation.getStatus()) && "CONFIRMED".equals(evaluation.getStatus().getCode())){
				//判断当前状态是否已确认 若是则跳转到查看页面
				return "redirect:/evaluation/evaluation/opt-query/queryEvaluationDetail.do?id="+evaluation.getId()+"&command=VIEW";
			}*/
		}
		
		List<EvaluationDetail> detailList=new ArrayList<EvaluationDetail>();
		//获取测评明细列表
		if(DataUtil.isNotNull(evaluation)){
			detailList=this.stuEvaluationservice.getEvaluationDetailById(evaluation.getId());
		}
		
		StudentInfoModel student=this.studentCommonService.queryStudentById(userId);
		
		Dic yearDic=this.stuEvaluationservice.getDicById(yearId);
		Dic termDic=this.stuEvaluationservice.getDicById(termId);
		Dic monthDic=this.stuEvaluationservice.getDicById(monthId);
		List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		
		model.addAttribute("yearDic", yearDic);
		model.addAttribute("termDic", termDic);
		model.addAttribute("monthDic", monthDic);
		model.addAttribute("baseTypeList", baseTypeList);
		model.addAttribute("detailList", detailList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("student", student);
		log.info("查询综合明细测评!");
		return "/evaluation/stuEvaluation/stuEvaluationDetail";
	}
	/***
	 * 通过学年、学期、测评月份判断是否已存在测评记录
	 * @param model
	 * @param request
	 * @param yearId
	 * @param termId
	 * @param monthId
	 * @return
	 */
	 @RequestMapping(value={"/evaluation/evaluation/opt-query/checkStuEvaluation"},produces={"text/plain;charset=UTF-8"})
	 @ResponseBody
	 public String checkStuEvaluation(ModelMap model,HttpServletRequest request, String yearId, String monthId){
		 String userId=this.sessionUtil.getCurrentUserId();//当前登录人id
		 EvaluationInfo evaluation=this.stuEvaluationservice.getEvaluationInfo(yearId, monthId, userId);
		 if(DataUtil.isNotNull(evaluation)){
			 return "success";
		 }
		 return "fail";
	}
	
	/**
	 * 保存、提交测评
	 * @param model
	 * @param request
	 * @param evalutionScore
	 * @return
	 */
	@RequestMapping("/evaluation/evaluation/opt-update/saveEvaluation")
	public String saveEvaluation(ModelMap model, HttpServletRequest request, String id, String command){
		
		if(DataUtil.isNotNull(id)){//修改
			this.stuEvaluationservice.updateEvaluation(id, request, command);
			log.info("更新综合测评及明细!");
		}else{//新增
			this.stuEvaluationservice.saveEvaluation(request, command);
			log.info("保存综合测评及明细!");
		}
		
		return "redirect:/evaluation/evaluation/opt-query/queryEvaluationList.do";
	}

	/**
	 * 查看测评明细
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping({"/evaluation/evaluation/opt-query/queryEvaluationDetail"})
	public String queryEvaluationDetail(ModelMap model, String id, String command){
		String yearId="";
		String termId="";
		String monthId="";
		EvaluationInfo evaluation=new EvaluationInfo();
		if(DataUtil.isNotNull(id)){//列表页面 编辑
			evaluation=this.stuEvaluationservice.getEvaluationInfoById(id);
		}
		
		List<EvaluationDetail> detailList=new ArrayList<EvaluationDetail>();
		//获取测评明细列表
		if(DataUtil.isNotNull(evaluation)){
			yearId=evaluation.getYear().getId();
			termId=evaluation.getTerm().getId();
			monthId=evaluation.getMonth().getId();
			detailList=this.stuEvaluationservice.getEvaluationDetailById(evaluation.getId());
			
			model.addAttribute("student", evaluation.getStudent());
		}
		
		Dic yearDic=this.stuEvaluationservice.getDicById(yearId);
		Dic termDic=this.stuEvaluationservice.getDicById(termId);
		Dic monthDic=this.stuEvaluationservice.getDicById(monthId);
		List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		
		model.addAttribute("yearDic", yearDic);
		model.addAttribute("termDic", termDic);
		model.addAttribute("monthDic", monthDic);
		model.addAttribute("baseTypeList", baseTypeList);
		model.addAttribute("detailList", detailList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("command", command);
		log.info("查看测评明细!");
		return "/evaluation/stuEvaluation/viewStuEvaluationDetail";
	}
	
	/**
	 * 确认测评
	 * @param model
	 * @param request
	 * @param evalutionScore
	 * @return
	 */
	@RequestMapping("/evaluation/evaluation/opt-update/confirmEvaluation")
	public String confirmEvaluation(ModelMap model, HttpServletRequest request, String id){
		
		if(DataUtil.isNotNull(id)){//修改
			this.stuEvaluationservice.confirmEvaluation(id);
		}
		log.info("确认测评操作!");
		return "redirect:/evaluation/evaluation/opt-query/queryEvaluationList.do";
	}

	
	/**
	 * 删除测评
	 * @param model
	 * @param request
	 * @param response
	 * @param id
	 * @return
	 */
	@RequestMapping(value={"/evaluation/evaluation/opt-del/deleteEvaluation"},produces={"text/plain;charset=UTF-8"})
	@ResponseBody
	public String deleteEvaluation(ModelMap model,HttpServletRequest request,HttpServletResponse response,String id){
		String result="";
		try {
			if(DataUtil.isNotNull(id)){
				this.stuEvaluationservice.deleteEvaluationById(id);
			}
			result = "success";
		} catch (Exception e) {
			result = "error";
		}
		log.info("删除测评及其测评明细!");
		return result;
	}
	
	/**
	 * 判断是否是学生
	 * @param model
	 * @param request
	 * @param response
	 * @param id
	 * @return
	 */
	@RequestMapping(value={"/evaluation/evaluation/opt-query/checkStudent"},produces={"text/plain;charset=UTF-8"})
	@ResponseBody
	public String checkStudent(ModelMap model,HttpServletRequest request,HttpServletResponse response){
		String result="";
		try {
			boolean flag = ProjectSessionUtils.checkIsStudent(request);
			if(flag){
				result = "success";
			}else{
				result = "error";
			}
		} catch (Exception e) {
			result = "error";
		}
		return result;
	}
	
}
