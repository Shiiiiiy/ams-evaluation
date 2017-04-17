package com.uws.evaluation.controller;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.uws.common.service.IBaseDataService;
import com.uws.common.util.SchoolYearUtil;
import com.uws.comp.service.ICompService;
import com.uws.core.base.BaseController;
import com.uws.core.excel.ExcelException;
import com.uws.core.excel.ImportUtil;
import com.uws.core.excel.service.IExcelService;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.core.session.SessionFactory;
import com.uws.core.session.SessionUtil;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.base.BaseMajorModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationInfoVo;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.domain.evaluation.EvaluationTime;
import com.uws.evaluation.service.IEvaluationScoreService;
import com.uws.evaluation.service.IEvaluationSetService;
import com.uws.evaluation.service.IStuEvaluationService;
import com.uws.log.Logger;
import com.uws.log.LoggerFactory;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.FileUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.sys.service.impl.FileFactory;
import com.uws.sys.util.MultipartFileValidator;

@Controller
public class EvaluationScoreController extends BaseController{
	//log
	private Logger log=new LoggerFactory(StuEvaluationController.class);
	
	@Autowired
	private IEvaluationScoreService evaluationScoreService;
	
	@Autowired
	private IStuEvaluationService stuEvaluationservice;
	
	@Autowired
	private IEvaluationSetService evaluationSetService;
	
	@Autowired
	private IBaseDataService baseDataService;
	
	@Autowired
	private ICompService compService;
	
	@Autowired
	private IExcelService excelService;
	
	private FileUtil fileUtil=FileFactory.getFileUtil();
	
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	//sessionUtil工具类
	private SessionUtil sessionUtil = SessionFactory.getSession(com.uws.sys.util.Constants.MENUKEY_SYSCONFIG);
	
	/****
	 * 测评员或者辅导员查询测评记录
	 * @param model
	 * @param request
	 * @param evaluation
	 * @return
	 */
	@RequestMapping("/evaluation/score/opt-query/queryEvaluationList")
	public String queryEvaluationList(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation){
		log.info("查询综合测评!");
		List<BaseAcademyModel> collageList = this.baseDataService.listBaseAcademy();//学院列表
		List<BaseMajorModel> majorList = null;	//专业列表
		List<BaseClassModel> classList = null;	//班级列表
		List<Dic> schoolYearList = this.dicUtil.getDicInfoList("YEAR");	//学年
		List<Dic> termList = this.dicUtil.getDicInfoList("TERM");	//学期
		List<Dic> monthList = this.dicUtil.getDicInfoList("MONTH");	//测评月份
		Dic submitDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "SUBMIT");//提交状态
		Dic confirmDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "TO_CONFIRMED");//待确认状态
		List<Dic> statusList = new ArrayList<Dic>();	//状态
		statusList.add(submitDic);
		statusList.add(confirmDic);
		
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
		Dic termDic = SchoolYearUtil.getCurrentTermDic();
		Dic yearDic = SchoolYearUtil.getYearDic();
		if(null == evaluation.getYearId()){//默认当前学年
			evaluation.setYearId(yearDic.getId());
		}
		
		String userId=this.sessionUtil.getCurrentUserId();//当前登录人id
		//获取当前时间可以添加测评的测评月份
		List<EvaluationTime> evaluationTimeList=this.stuEvaluationservice.getEvaluationTimeByUserId(userId);
		//班级列表
		List<BaseClassModel> evaluationClassList = this.evaluationScoreService.queryEvaluationClassList(userId);	
		
		Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
		Page page = this.evaluationScoreService.queryEvaluationScorePage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation);
		
		model.addAttribute("termDic", termDic);
		model.addAttribute("yearDic", yearDic);
		model.addAttribute("collageList", collageList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("schoolYearList", schoolYearList);
		model.addAttribute("termList", termList);
		model.addAttribute("monthList", monthList);
		model.addAttribute("statusList", statusList);
		model.addAttribute("evaluationTimeList", evaluationTimeList);
		model.addAttribute("evaluationClassList", evaluationClassList);
		model.addAttribute("page", page);

		return "/evaluation/scoreEvaluation/evaluationScoreList";
	}
	
	/***
	 * 编辑测评信息
	 * @param model
	 * @param yearId
	 * @param termId
	 * @param monthId
	 * @return
	 */
	@RequestMapping({"evaluation/score/opt-update/editEvaluationInfo"})
	public String getEvaluationDetail(ModelMap model,HttpServletRequest request, String id){
		String yearId="";
		String termId="";
		String monthId="";
		EvaluationInfo evaluation=new EvaluationInfo();
		String nextEvaluationId=request.getParameter("nextEvaluationId");
		
		if(DataUtil.isNotNull(id)){//列表页面 编辑
			evaluation=this.stuEvaluationservice.getEvaluationInfoById(id);
			if(DataUtil.isNotNull(evaluation)){
				yearId=evaluation.getYear().getId();
				termId=evaluation.getTerm().getId();
				monthId=evaluation.getMonth().getId();
			}
		}
		
		List<EvaluationDetail> detailList=new ArrayList<EvaluationDetail>();
		//获取测评明细列表和该测评的下一个未录分的测评信息
		if(DataUtil.isNotNull(evaluation)){
			detailList=this.stuEvaluationservice.getEvaluationDetailById(evaluation.getId());
			model.addAttribute("student", evaluation.getStudent());
			
			if(DataUtil.isNotNull(nextEvaluationId) && !"null".equals(nextEvaluationId)){
				model.addAttribute("nextEvaluationId", nextEvaluationId);
			}else{
				//查询下一个测评信息
				EvaluationInfo nextEvaluation=this.evaluationScoreService.getNextEvaluation(evaluation);
				if(DataUtil.isNotNull(nextEvaluation)){
					model.addAttribute("nextEvaluationId", nextEvaluation.getId());
				}
			}
			
		}
		
		Dic yearDic=this.stuEvaluationservice.getDicById(yearId);
		Dic termDic=this.stuEvaluationservice.getDicById(termId);
		Dic monthDic=this.stuEvaluationservice.getDicById(monthId);
		
		List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
		List<Dic> scoreTypeList=dicUtil.getDicInfoList("EVALUATION_SCORE_TYPE");//测评分分数类型 
		
		List<EvaluationScore> evaluationScoreList=this.evaluationSetService.queryEvaluationScore();//测评分基础设置
		
		model.addAttribute("yearDic", yearDic);
		model.addAttribute("termDic", termDic);
		model.addAttribute("monthDic", monthDic);
		model.addAttribute("detailList", detailList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("baseTypeList", baseTypeList);
		model.addAttribute("scoreTypeList", scoreTypeList);
		model.addAttribute("evaluationScoreList", evaluationScoreList);
	
		return "/evaluation/scoreEvaluation/editEvaluationDetail";
	}
	
	/**
	 * 测评评分
	 * @param model
	 * @param request
	 * @param evalutionScore
	 * @return
	 */
	@RequestMapping("/evaluation/score/opt-update/updateEvaluationDetail")
	public String updateEvaluationDetail(ModelMap model, HttpServletRequest request, String id,
			String command, String nextEvaluationId){
		if(DataUtil.isNotNull(id)){//修改测评明细和总分
			this.stuEvaluationservice.updateEvaluation(id, request, command);
		}
		if("CONFIRMED".equals(command)){//修改已确认分后跳转测评修改页面（测评辅导员）
			return "redirect:/evaluation/update/opt-query/queryConfirmEvaluationList.do";
		}
		if(!("".equals(nextEvaluationId)) && DataUtil.isNotNull(nextEvaluationId)){
			//存在下一条记录则修改后直接进入下一条评分记录
			return "redirect:/evaluation/score/opt-update/editEvaluationInfo.do?id="+nextEvaluationId;
		}else{//不存在下一条则跳转回测评记录列表页
			return "redirect:/evaluation/score/opt-query/queryEvaluationList.do";
		}
	}
	
	/**
	 * 查看测评明细
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping({"/evaluation/score/opt-query/queryEvaluationDetail"})
	public String queryEvaluationDetail(ModelMap model, String id){
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
		
		return "/evaluation/scoreEvaluation/viewEvaluationDetail";
	}
	
	/***
	 * 导出预处理
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping({"/evaluation/score/opt-query/nsm/exportEvaluationDetailList"})
	  public String exportUserList(ModelMap model, HttpServletRequest request){
	    int exportSize = Integer.valueOf(request.getParameter("exportSize")).intValue();
	    int pageTotalCount = Integer.valueOf(request.getParameter("pageTotalCount")).intValue();
	    int maxNumber = 0;
	    if (pageTotalCount < exportSize)
	      maxNumber = 1;
	    else if (pageTotalCount % exportSize == 0)
	      maxNumber = pageTotalCount / exportSize;
	    else {
	      maxNumber = pageTotalCount / exportSize + 1;
	    }
	    model.addAttribute("exportSize", Integer.valueOf(exportSize));
	    model.addAttribute("maxNumber", Integer.valueOf(maxNumber));
	    if (maxNumber <= 500)
	      model.addAttribute("isMore", "false");
	    else {
	      model.addAttribute("isMore", "true");
	    }
	    return "/evaluation/scoreEvaluation/exportEvaluationDetailList";
	  }
	
	  /***
	   * 导出综合测评明细
	   * @param model
	   * @param request
	   * @param evaluation
	   * @param response
	   */
	  @RequestMapping({"/evaluation/score/opt-query/exportEvaluationDetail"})
	  public void exportEvaluationDetail(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation, HttpServletResponse response){
		  String exportSize = request.getParameter("evaluationQuery_exportSize");
		    String exportPage = request.getParameter("evaluationQuery_exportPage");
		    Page page=this.evaluationScoreService.queryEvaluationScorePage(Integer.parseInt(exportPage), Integer.parseInt(exportSize), evaluation);

		    List listMap = new ArrayList();
		    List<EvaluationInfoVo> evaluationList = (List)page.getResult();
		    for(EvaluationInfoVo eval : evaluationList) {
		    	List<EvaluationDetail> detailList=this.stuEvaluationservice.getEvaluationDetailById(eval.getId());
		    	 for(EvaluationDetail detail : detailList) {
				    	Map map = new HashMap();
				    	if(DataUtil.isNotNull(detail.getReason()) && !"".equals(detail.getReason())){
				    		map.put("college", eval.getStudent().getCollege().getName());
					    	map.put("major", eval.getStudent().getMajor().getMajorName());
					    	map.put("class", eval.getStudent().getClassId().getClassName());
					    	map.put("student", eval.getStudent().getName());
					    	map.put("studentNo", eval.getStudent().getStuNumber());
					    	map.put("year", eval.getYear().getName());
					    	map.put("term", eval.getTerm().getName());
					    	map.put("month", eval.getMonth().getName());
					    	map.put("type", detail.getType().getName());
					    	map.put("reason", detail.getReason());
					    	map.put("score", detail.getScore());
					    	
					    	listMap.add(map);
				    	}
				    }
		    }
		    
		    HSSFWorkbook wb;
			try {
				wb = this.excelService.exportData("export_evaluation_detail.xls", "exportEvaluationDetail", listMap);
				String filename = "测评明细" + exportPage + ".xls";
			    response.setContentType("application/x-excel");
			    response.setHeader("Content-disposition", "attachment;filename=" + new String(filename.getBytes("GBK"), "iso-8859-1"));
			    response.setCharacterEncoding("UTF-8");
			    OutputStream ouputStream = response.getOutputStream();
			    wb.write(ouputStream);
			    ouputStream.flush();
			    ouputStream.close();
			} catch (ExcelException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
	  }
	  
	  /** 
		* @Title: importData 
		* @Description:  执行导入
		* @param  @param model
		* @param  @param session
		* @param  @param compareId
		* @param  @return    
		* @return String    
		* @throws 
		*/
		@SuppressWarnings("unchecked")
		@RequestMapping({"/evaluation/score/opt-query/importEvaluationDetail"})
		public String importPunish(ModelMap model, @RequestParam("file") MultipartFile file, String maxSize, String allowedExt, 
				HttpServletRequest request, HttpSession session) throws Exception {
			
			List errorText = new ArrayList();
			String errorTemp = "";
			MultipartFileValidator validator = new MultipartFileValidator();
			if(DataUtil.isNotNull(allowedExt)) {
				validator.setAllowedExtStr(allowedExt.toLowerCase());
			}
			if(DataUtil.isNotNull(maxSize)) {
				validator.setMaxSize(Long.valueOf(maxSize).longValue());
			}else{
				validator.setMaxSize(20971520);
			}
			String returnValue = validator.validate(file);
			if(!returnValue.equals("")) {
				errorTemp = returnValue;
				errorText.add(errorTemp);
				model.addAttribute("errorText", errorText.size()==0 ? null : errorText);
			    model.addAttribute("importFlag", Boolean.valueOf(true));
			    return "/evaluation/scoreEvaluation/importEvaluation";
			}else{
				String tempFileId = this.fileUtil.saveSingleFile(true, file);
				File tempFile = this.fileUtil.getTempRealFile(tempFileId);
				String filePath = tempFile.getAbsolutePath();
				session.setAttribute("filePath", filePath);
				try {
					ImportUtil iu = new ImportUtil();
					//导入
					List<EvaluationDetail> list = iu.getDataList(filePath, "importEvaluationDetail", null, EvaluationDetail.class);        //Excel数据
					this.evaluationScoreService.importData(list);
				} catch (OfficeXmlFileException e) {
					
					e.printStackTrace();
					errorTemp = "OfficeXmlFileException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (IOException e) {
					
					e.printStackTrace();
					errorTemp = "IOException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (IllegalAccessException e) {
					
					e.printStackTrace();
					errorTemp = "IllegalAccessException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (ExcelException e) {
					
					e.printStackTrace();
					errorTemp = e.getMessage();
					errorText.add(errorTemp);
				} catch (InstantiationException e) {
					
					e.printStackTrace();
					errorTemp = "InstantiationException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (ClassNotFoundException e) {
					
					e.printStackTrace();
				}
				model.addAttribute("importFlag", Boolean.valueOf(true));
				model.addAttribute("errorText", errorText.size()==0 ? null : errorText);
				return "/evaluation/scoreEvaluation/importEvaluation";
			}
		}
	
		/** 
		* @Title: importHistoryData 
		* @Description:  导入历史数据
		* @param  @param model
		* @param  @param session
		* @param  @param compareId
		* @param  @return    
		* @return String    
		* @throws 
		*/
		@SuppressWarnings("unchecked")
		@RequestMapping({"/evaluation/score/opt-query/importHistoryData"})
		public String importHistoryData(ModelMap model, @RequestParam("file") MultipartFile file, String maxSize, String allowedExt, 
				HttpServletRequest request, HttpSession session) throws Exception {
			
			List errorText = new ArrayList();
			String errorTemp = "";
			MultipartFileValidator validator = new MultipartFileValidator();
			if(DataUtil.isNotNull(allowedExt)) {
				validator.setAllowedExtStr(allowedExt.toLowerCase());
			}
			if(DataUtil.isNotNull(maxSize)) {
				validator.setMaxSize(Long.valueOf(maxSize).longValue());
			}else{
				validator.setMaxSize(20971520);
			}
			String returnValue = validator.validate(file);
			if(!returnValue.equals("")) {
				errorTemp = returnValue;
				errorText.add(errorTemp);
				model.addAttribute("errorText", errorText.size()==0 ? null : errorText);
			    model.addAttribute("importFlag", Boolean.valueOf(true));
			    return "/evaluation/scoreEvaluation/importEvaluation";
			}else{
				String tempFileId = this.fileUtil.saveSingleFile(true, file);
				File tempFile = this.fileUtil.getTempRealFile(tempFileId);
				String filePath = tempFile.getAbsolutePath();
				session.setAttribute("filePath", filePath);
				try {
					ImportUtil iu = new ImportUtil();
					//导入旧数据
					List<EvaluationInfo> list = iu.getDataList(filePath, "importEvaluation", null, EvaluationInfo.class);
					this.evaluationScoreService.importEvaluationData(list);
				} catch (OfficeXmlFileException e) {
					
					e.printStackTrace();
					errorTemp = "OfficeXmlFileException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (IOException e) {
					
					e.printStackTrace();
					errorTemp = "IOException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (IllegalAccessException e) {
					
					e.printStackTrace();
					errorTemp = "IllegalAccessException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (ExcelException e) {
					
					e.printStackTrace();
					errorTemp = e.getMessage();
					errorText.add(errorTemp);
				} catch (InstantiationException e) {
					
					e.printStackTrace();
					errorTemp = "InstantiationException" + e.getMessage();
					errorText.add(errorTemp);
				} catch (ClassNotFoundException e) {
					
					e.printStackTrace();
				}
				model.addAttribute("importFlag", Boolean.valueOf(true));
				model.addAttribute("errorText", errorText.size()==0 ? null : errorText);
				return "/evaluation/scoreEvaluation/importEvaluation";
			}
		}
		
		/****
		 * 辅导员查询已确认测评记录
		 * @param model
		 * @param request
		 * @param evaluation
		 * @return
		 */
		@RequestMapping("/evaluation/update/opt-query/queryConfirmEvaluationList")
		public String queryConfirmEvaluationList(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation){
			log.info("查询综合测评!");
			List<BaseAcademyModel> collageList = this.baseDataService.listBaseAcademy();//学院列表
			List<BaseMajorModel> majorList = null;	//专业列表
			List<BaseClassModel> classList = null;	//班级列表
			List<Dic> schoolYearList = this.dicUtil.getDicInfoList("YEAR");	//学年
			List<Dic> termList = this.dicUtil.getDicInfoList("TERM");	//学期
			List<Dic> monthList = this.dicUtil.getDicInfoList("MONTH");	//测评月份
			Dic confirmDic=this.dicUtil.getDicInfo("EVALUATION_STATUS", "CONFIRMED");//已确认状态
			List<Dic> statusList = new ArrayList<Dic>();	//状态
			statusList.add(confirmDic);
			
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
			Dic yearDic = SchoolYearUtil.getYearDic();
			if(null == evaluation.getYearId()){//默认当前学年
				evaluation.setYearId(yearDic.getId());
			}
			
			Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
			Page page = this.evaluationScoreService.queryConfirmEvaluationList(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation);
			
			model.addAttribute("collageList", collageList);
			model.addAttribute("majorList", majorList);
			model.addAttribute("classList", classList);
			model.addAttribute("evaluation", evaluation);
			model.addAttribute("schoolYearList", schoolYearList);
			model.addAttribute("termList", termList);
			model.addAttribute("monthList", monthList);
			model.addAttribute("statusList", statusList);
			model.addAttribute("page", page);

			return "/evaluation/scoreEvaluation/evaluationConfirmList";
		}
		
		/**
		 * 确认测评分修改
		 * @param model
		 * @param request
		 * @param evalutionScore
		 * @return
		 */
		@RequestMapping("/evaluation/confirm/opt-update/editEvaluationInfo")
		public String getEvaluationInfo(ModelMap model,HttpServletRequest request, String id){
			String yearId="";
			String termId="";
			String monthId="";
			EvaluationInfo evaluation=new EvaluationInfo();
			
			if(DataUtil.isNotNull(id)){//列表页面 编辑
				evaluation=this.stuEvaluationservice.getEvaluationInfoById(id);
				if(DataUtil.isNotNull(evaluation)){
					yearId=evaluation.getYear().getId();
					termId=evaluation.getTerm().getId();
					monthId=evaluation.getMonth().getId();
				}
			}
			
			List<EvaluationDetail> detailList=new ArrayList<EvaluationDetail>();
			//获取测评明细列表和该测评的下一个未录分的测评信息
			if(DataUtil.isNotNull(evaluation)){
				detailList=this.stuEvaluationservice.getEvaluationDetailById(evaluation.getId());
				model.addAttribute("student", evaluation.getStudent());
			}
			
			Dic yearDic=this.stuEvaluationservice.getDicById(yearId);
			Dic termDic=this.stuEvaluationservice.getDicById(termId);
			Dic monthDic=this.stuEvaluationservice.getDicById(monthId);
			
			List<Dic> baseTypeList=dicUtil.getDicInfoList("EVALUATION_BASE_TYPE");//测评分基础类型 
			List<Dic> scoreTypeList=dicUtil.getDicInfoList("EVALUATION_SCORE_TYPE");//测评分分数类型 
			
			List<EvaluationScore> evaluationScoreList=this.evaluationSetService.queryEvaluationScore();//测评分基础设置
			
			model.addAttribute("yearDic", yearDic);
			model.addAttribute("termDic", termDic);
			model.addAttribute("monthDic", monthDic);
			model.addAttribute("detailList", detailList);
			model.addAttribute("evaluation", evaluation);
			model.addAttribute("baseTypeList", baseTypeList);
			model.addAttribute("scoreTypeList", scoreTypeList);
			model.addAttribute("evaluationScoreList", evaluationScoreList);
		
			return "/evaluation/scoreEvaluation/editConfirmEvaluationDetail";
		}
		
		/**
		 * 给班级添加测评记录
		 * @param model
		 * @param request
		 * @param evalutionScore
		 * @return
		 */
		@RequestMapping(value={"/evaluation/evaluation/add/addClassEvaluation"},produces={"text/plain;charset=UTF-8"})
		@ResponseBody
		public String addClassEvaluation(ModelMap model,HttpServletRequest request, String yearId, String termId,
				String monthId, String classId){
			this.evaluationScoreService.addClassEvaluation(yearId, termId, monthId, classId);
			
			return "success";
		}
}
