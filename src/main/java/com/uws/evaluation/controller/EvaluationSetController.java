package com.uws.evaluation.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.uws.common.service.IBaseDataService;
import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.common.util.JsonUtils;
import com.uws.comp.service.ICompService;
import com.uws.core.base.BaseController;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.base.BaseMajorModel;
import com.uws.domain.base.BaseTeacherModel;
import com.uws.domain.base.JsonModel;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTerm;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.domain.evaluation.EvaluationUser;
import com.uws.domain.teacher.StuJobTeamSetModel;
import com.uws.evaluation.service.IEvaluationSetService;
import com.uws.log.Logger;
import com.uws.log.LoggerFactory;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.user.model.User;
import com.uws.user.service.IUserService;

/**
 * @Description 综合测评基础设置
 * @author Jiangbl
 * @date 2015-8-13
 */

@Controller
public class EvaluationSetController extends BaseController{
	
	// 日志
	private Logger log = new LoggerFactory(EvaluationSetController.class);
	
	@Autowired
	private IEvaluationSetService evaluationSetService;
	
	@Autowired
	private IBaseDataService baseDataService;
	
	@Autowired
	private IUserService userService;
	
	@Autowired
	private ICompService compService;
	
	@Autowired
	private IStuJobTeamSetCommonService jobTeamService;
	
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	/**
	 * 查询基础分设置
	 * @return
	 */
	@RequestMapping("/evaluation/scoreSet/opt-query/queryEvaluationScore")
	public String queryEvaluationScore(ModelMap model, HttpServletRequest request, EvaluationScore evaluationScore){
		log.info("查询已设置的基础分");
		List<EvaluationScore> evaluationScoreList=this.evaluationSetService.queryEvaluationScore();
		List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		List<Dic> scoreTypeList=dicUtil.getDicInfoList("EVALUATION_SCORE_TYPE");//测评分分数类型 
		
		model.addAttribute("baseTypeList", baseTypeList);
		model.addAttribute("scoreTypeList", scoreTypeList);
		model.addAttribute("evaluationScoreList", evaluationScoreList);
		return "/evaluation/baseSet/editEvaluationScore";
	}
	
	/**
	 * 编辑基础分
	 * @param model
	 * @param request
	 * @param evalutionScore
	 * @return
	 */
	@RequestMapping("/evaluation/scoreSet/opt-query/saveEvaluationScore")
	public String saveEvaluationScore(ModelMap model, HttpServletRequest request){
		List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		List<Dic> scoreTypeList=dicUtil.getDicInfoList("EVALUATION_SCORE_TYPE");//测评分分数类型 
		
		//按照测评分基础类型、测评分分数类型逐一保存基础设置分值
		for (Iterator iterator = baseTypeList.iterator(); iterator.hasNext();) {
			Dic baseTypeDic = (Dic) iterator.next();
			for (Iterator iterator2 = scoreTypeList.iterator(); iterator2.hasNext();) {
				Dic scoreTypeDic = (Dic) iterator2.next();
				String id=request.getParameter(baseTypeDic.getCode()+scoreTypeDic.getId());
				String score=request.getParameter(baseTypeDic.getCode()+scoreTypeDic.getCode());
				User creator=new User(this.sessionUtil.getCurrentUserId());//当前登录人
				
				if(DataUtil.isNotNull(id)){//更新
					EvaluationScore evaluationScore=new EvaluationScore();
					evaluationScore=this.evaluationSetService.getEvaluationScoreById(id);
					evaluationScore.setScore(score);
					evaluationScore.setCreator(creator);
					
					this.evaluationSetService.updateEvaluationScore(evaluationScore);
					log.info("修改基础分");
				}else{//新增
					EvaluationScore evaluationScore=new EvaluationScore();
					evaluationScore.setBaseType(baseTypeDic);
					evaluationScore.setScoreType(scoreTypeDic);
					evaluationScore.setScore(score);
					evaluationScore.setCreator(creator);
					
					this.evaluationSetService.saveEvaluationScore(evaluationScore);
					log.info("新增基础分");
				}
			}
		}
		
		return "redirect:/evaluation/scoreSet/opt-query/queryEvaluationScore.do";
	}
	
	/**
	 * 综合测评时间设置查询
	 * @param model
	 * @param request
	 * @param evaluationTime
	 * @return
	 */
	@RequestMapping("/evaluation/timeSet/opt-query/queryEvaluationTime")
	public String queryEvaluationTimePage(ModelMap model, HttpServletRequest request, EvaluationTime evaluationTime){
		log.info("综合测评时间设置查询！");
		String currentUserId=this.sessionUtil.getCurrentUserId();
		if(this.jobTeamService.isEvaCounsellor(currentUserId)){//判断是否测评辅导员 只查看自己相关学院的
			List<BaseAcademyModel> collegeList=this.jobTeamService.getBAMByTeacherId(currentUserId);
			model.addAttribute("collegeList", collegeList);
		}else{
			List<BaseAcademyModel> collegeList = baseDataService.listBaseAcademy();//学院
			model.addAttribute("collegeList", collegeList);
		}
    	List<Dic> monthList=dicUtil.getDicInfoList("MONTH");//月份
    	getMapCollegeEvaCounsellor(model);
    	
    	int pageNo = request.getParameter("pageNo")!=null?Integer.parseInt(request.getParameter("pageNo")):1;
		Page page = this.evaluationSetService.queryEvaluationTimePage(pageNo, 12, evaluationTime, currentUserId);
		
    	model.addAttribute("evaluationTime", evaluationTime);
    	model.addAttribute("monthList", monthList);
    	model.addAttribute("page", page);
		return "/evaluation/baseSet/evaluationTimeList";
	}
	
	/**
	 * 编辑测评时间设置
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping({"/evaluation/timeSet/opt-add/editEvaluationTime","/evaluation/timeSet/opt-update/editEvaluationTime"})
	public String editEvaluationTime(ModelMap model, HttpServletRequest request){
		String id=request.getParameter("id");
		String currentUserId=this.sessionUtil.getCurrentUserId();
		
		if(DataUtil.isNotNull(id)){//编辑
			EvaluationTime evaluationTime=this.evaluationSetService.getEvaluationTimeById(id);
			model.addAttribute("evaluationTime", evaluationTime);
		}else{//新增
			EvaluationTime evaluationTime=new EvaluationTime();
			model.addAttribute("evaluationTime", evaluationTime);
		}
		if(this.jobTeamService.isEvaCounsellor(currentUserId)){//判断是否测评辅导员 只查看自己相关学院的
			List<BaseAcademyModel> collegeList=this.jobTeamService.getBAMByTeacherId(currentUserId);
			model.addAttribute("collegeList", collegeList);
		}else{
			List<BaseAcademyModel> collegeList = baseDataService.listBaseAcademy();//学院
			model.addAttribute("collegeList", collegeList);
		}
		
    	List<Dic> monthList=dicUtil.getDicInfoList("MONTH");//月份
    	
    	model.addAttribute("monthList", monthList);
    	getMapCollegeEvaCounsellor(model);
		return "/evaluation/baseSet/editEvaluationTime";
	}
	
	/**
	 * 保存综合测评时间设置
	 * @param model
	 * @param request
	 * @param evaluationTime
	 * @return
	 * @throws ParseException 
	 */
	@RequestMapping({"/evaluation/timeSet/opt-add/saveEvaluationTime","/evaluation/timeSet/opt-update/saveEvaluationTime"})
	public String saveEvaluationTime(ModelMap model, HttpServletRequest request, EvaluationTime evaluationTime) throws ParseException{
		//时间处理
		SimpleDateFormat date=new SimpleDateFormat("yyyy-MM-dd");
		String addStartTime=request.getParameter("addStartDate");
		String addEndTime=request.getParameter("addEndDate");
		String updateStartTime=request.getParameter("updateStartDate");
		String updateEndTime=request.getParameter("updateEndDate");
		
		evaluationTime.setAddStartTime(date.parse(addStartTime));
		evaluationTime.setAddEndTime(date.parse(addEndTime));
		evaluationTime.setUpdateStartTime(date.parse(updateStartTime));
		evaluationTime.setUpdateEndTime(date.parse(updateEndTime));
		
		if(DataUtil.isNotNull(evaluationTime.getId())){
			this.evaluationSetService.updateEvaluationTime(evaluationTime);
			log.info("修改综合测评时间设置！");
		}else{
			this.evaluationSetService.saveEvaluationTime(evaluationTime);
			log.info("新增综合测评时间设置！");
		}
		
		return "redirect:/evaluation/timeSet/opt-query/queryEvaluationTime.do";
	}
	
	/**
	 * 获取当前选择学院的辅导员
	 * @param model
	 * @param request
	 * @param collegeId
	 * @return
	 */
	 @RequestMapping(value={"/evaluation/timeSet/opt-query/getInstructor"},produces={"text/plain;charset=UTF-8"})
	 @ResponseBody
	 public String getInstructor(ModelMap model,HttpServletRequest request,String collegeId){
		 User user=new User();
		 //BaseTeacherModel instructor=this.jobTeamService.getEvaCounsellorByCollegeId(collegeId);
		 List<BaseTeacherModel> instructorList=this.jobTeamService.getEvaCounsellorByCollegeId(collegeId);
		 String id="";
		 String name = "";
		 for (Iterator iterator = instructorList.iterator(); iterator.hasNext();) {
			BaseTeacherModel baseTeacherModel = (BaseTeacherModel) iterator.next();
			id+=baseTeacherModel.getId()+";";
			name+=baseTeacherModel.getName()+";";
		 }
		 if(!"".equals(id) && !"".equals(name)){
			 user.setId(id.substring(0, id.length()-1));
			 user.setName(name.substring(0, name.length()-1));
		 }
		 JSONObject json=JsonUtils.getJsonObject(user);
		 
		 return JsonUtils.jsonObject2Json(json);
	}

	/**
	 * 删除测评设置月份
	 * @param model
	 * @param request
	 * @param response
	 * @param id
	 * @return
	 */
	@RequestMapping(value={"/evaluation/timeSet/opt-del/deleteEvaluationTime"},produces={"text/plain;charset=UTF-8"})
	@ResponseBody
	public String deleteEvaluationTime(ModelMap model,HttpServletRequest request,HttpServletResponse response,String id){
		String result="";
		try {
			if(DataUtil.isNotNull(id)){
				this.evaluationSetService.deleteEvaluationTimeById(id);
			}
			result = "success";
		} catch (Exception e) {
			result = "error";
		}
		return result;
	}
	
	/**
	 * 测评月份判重
	 * @param model
	 * @param request
	 * @param collegeId
	 * @param monthId
	 * @return
	 */
	@RequestMapping(value={"/evaluation/timeSet/opt-query/comfirmEvaluationTime"},produces={"text/plain;charset=UTF-8"})
	@ResponseBody
	public String comfirmEvaluationTime(ModelMap model,HttpServletRequest request,String collegeId, String monthId, String id){
		Boolean flag=this.evaluationSetService.getEvaluationTime(collegeId, monthId, id);
		if("".equals(collegeId) || "".equals(monthId)){
			return "success";
		}
		if(!flag){
			return "success";
		}else{
			return "error";
		}
	}
	
	/**
	 * 获取所有班级的测评员
	 * @param model
	 * @param request
	 * @param evaluationuser
	 * @return
	 */
	@RequestMapping("/evaluation/userSet/opt-query/queryEvaluationUser")
	public String queryEvaluationUser(ModelMap model, HttpServletRequest request, EvaluationUser evaluationUser){
		Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
		Page page = this.evaluationSetService.queryClassEvaluationUserPage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluationUser);
		
		// 下拉列表 学院
		List<BaseAcademyModel> collageList = baseDataService.listBaseAcademy();
		List<BaseMajorModel> majorList = null;
		List<BaseClassModel> classList = null;
		if (null != evaluationUser) {
			if (null != evaluationUser.getCollageId()) {
				majorList = compService.queryMajorByCollage(evaluationUser.getCollageId());
				log.debug("若已经选择学院，则查询学院下的专业信息.");
			}
			// 下拉列表 班级
			if (null != evaluationUser.getMajorId()) {
				classList = compService.queryClassByMajor(evaluationUser.getMajorId());
				log.debug("若已经选择专业，则查询专业下的班级信息.");
			}
		}
				
		model.addAttribute("collageList", collageList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
		model.addAttribute("evaluationUser", evaluationUser);
		model.addAttribute("page", page);
		return "/evaluation/baseSet/evaluationUserList";
	}
	
	/**
	 * 修改测评员
	 * @param baseClass
	 * @param model
	 * @param fileId
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value={"/evaluation/userSet/opt-edit/updateEvaluationUser"} , produces={"text/plain;charset=UTF-8"})
	public String updateEvaluationUser( String studentId, String classId) {
		EvaluationUser evaluationUser = this.evaluationSetService.queryEvaluationUser(classId);
		if(null != evaluationUser){//修改测评员
			this.evaluationSetService.updateEvaluationUser(evaluationUser, studentId);
		}else{//新增测评员
			this.evaluationSetService.saveEvaluationUser(studentId, classId);
		}
		
		return "success";
	}
	
	/**
	 * 查看用户基础信息
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping({"/evaluation/userSet/opt-query/nsm/viewUser"})
	public String viewUser(ModelMap model, HttpServletRequest request){
	    String id = request.getParameter("id");
	    User user = null;
	    user = this.userService.getUserById(id);
	    if (user == null)
	      model.addAttribute("user", new User());
	    else {
	      model.addAttribute("user", user);
	    }
	    model.addAttribute("tabActive", "user");
	    return "/evaluation/baseSet/viewUser";
	 }
	
	/**
	 * 查询学期、月份设置
	 * @return
	 */
	@RequestMapping("/evaluation/termSet/opt-query/queryEvaluationTerm")
	public String queryEvaluationTerm(ModelMap model, HttpServletRequest request){
		log.info("查询测评学期设置");
		List<Dic> termList = this.dicUtil.getDicInfoList("TERM");	//学期
		List<Dic> monthList = this.dicUtil.getDicInfoList("MONTH");	//测评月份
		List<EvaluationTerm> evaluationTermList=this.evaluationSetService.queryEvaluationTerm();
		
		model.addAttribute("termList", termList);
		model.addAttribute("monthList", monthList);
		model.addAttribute("evaluationTermList", evaluationTermList);
		return "/evaluation/baseSet/editEvaluationTerm";
	}
	
	/**
	 * 保存测评学期、月份设置
	 * @param model
	 * @param request
	 * @param evalutionScore
	 * @return
	 */
	@RequestMapping("/evaluation/scoreSet/opt-query/saveEvaluationTerm")
	public String saveEvaluationTerm(ModelMap model, HttpServletRequest request){
		
		this.evaluationSetService.saveEvaluationTerm(request);
		return "redirect:/evaluation/termSet/opt-query/queryEvaluationTerm.do";
	}
	
	/***
	 * 通过测评月份查询所在学期
	 * @param model
	 * @param request
	 * @param key
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value={"/evaluation/baseSet/query/queryEvaluationTermByMonthId"}, produces={"text/plain;charset=UTF-8"})
	public String queryEvaluationTermByMonthId(ModelMap model, HttpServletRequest request,String monthId) 
	{
		List<EvaluationTerm> list = this.evaluationSetService.queryEvaluationTerm();
		List<JsonModel> jsonList = new ArrayList<JsonModel>();
		if (list!= null && list.size() > 0) {
			JsonModel json = null;
			for (EvaluationTerm c: list){
				if(monthId.equals(c.getMonth().getId())){
					json = new JsonModel();
					json.setId(c.getTerm().getId());
					json.setName(c.getTerm().getName());
					jsonList.add(json);
				}
			}
		}
		String result = JSONArray.fromObject(jsonList).toString();
		return result;
	}
	
	/****
	 * 实时调用学工查询测评辅导员接口
	 * @param model
	 */
	private void getMapCollegeEvaCounsellor(ModelMap model){
		List<StuJobTeamSetModel> list=this.jobTeamService.getMapCollegeEvaCounsellor();
		Map<String, Object> map=new HashMap<String, Object>();
		String collegeId = "";
		for (StuJobTeamSetModel m : list) {
			collegeId = m.getCollege().getId();
			if(map.containsKey(collegeId))
				map.put(collegeId,map.get(collegeId)+";"+ m.getTeacher().getName());
			else
				map.put(m.getCollege().getId(), m.getTeacher().getName());
        }
		model.addAttribute("map", map);
	}
	
}
