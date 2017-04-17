package com.uws.evaluation.controller;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.uws.common.service.IBaseDataService;
import com.uws.common.util.SchoolYearUtil;
import com.uws.comp.service.ICompService;
import com.uws.core.base.BaseController;
import com.uws.core.excel.ExcelException;
import com.uws.core.excel.service.IExcelService;
import com.uws.core.hibernate.dao.support.Page;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.base.BaseMajorModel;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationInfoVo;
import com.uws.domain.evaluation.EvaluationScore;
import com.uws.evaluation.service.IEvaluationSetService;
import com.uws.evaluation.service.IEvaluationStatisticService;
import com.uws.log.Logger;
import com.uws.log.LoggerFactory;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;

@Controller
public class EvaluationStatisticController extends BaseController{
	//log
	private Logger log=new LoggerFactory(EvaluationQueryController.class);
	
	@Autowired
	public IEvaluationStatisticService evaluationStatisticService;
	
	@Autowired
	private IEvaluationSetService evaluationSetService;
	
	@Autowired
	private IBaseDataService baseDataService;
	
	@Autowired
	private ICompService compService;
	
	@Autowired
	private IExcelService excelService;
	
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	
	/***
	 * 测评统计
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping("/evaluation/statistics/opt-query/statisticsEvaluation")
	public String statisticEvaluation(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation, String flag){
		
		List<BaseAcademyModel> collageList = this.baseDataService.listBaseAcademy();//学院列表
		List<BaseMajorModel> majorList = null;	//专业列表
		List<BaseClassModel> classList = null;	//班级列表
		List<Dic> schoolYearList = this.dicUtil.getDicInfoList("YEAR");	//学年
		//List<Dic> termList = this.dicUtil.getDicInfoList("TERM");	//学期
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
		
		if(null == evaluation.getYearId()){//默认当前学年
			Dic yearDic = SchoolYearUtil.getYearDic();
			evaluation.setYearId(yearDic.getId());
		}
		
		Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
		Page page=new Page();
		if(!"1".equals(flag)){
			page = this.evaluationStatisticService.statisticEvaluationPage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation, request);
		}
		
		model.addAttribute("collageList", collageList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
		model.addAttribute("schoolYearList", schoolYearList);
		//model.addAttribute("termList", termList);
		model.addAttribute("monthList", monthList);
		model.addAttribute("evaluation", evaluation);
		
		model.addAttribute("page", page);
		log.info("测评统计查询!");
		return "/evaluation/statistic/statisticEvaluation";
	}

	/***
	 * 导出预处理
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping({"/evaluation/statistics/opt-query/nsm/exportEvaluationList"})
	  public String exportEvaluationList(ModelMap model, HttpServletRequest request)
	  {
	    int exportSize = Integer.valueOf(request.getParameter("exportSize")).intValue();
	    int pageTotalCount = Integer.valueOf(request.getParameter("pageTotalCount")).intValue();
	    int maxNumber = 0;
	    if(pageTotalCount < exportSize){
	    	maxNumber = 1;
	    }else if(pageTotalCount % exportSize == 0){
	    	maxNumber = pageTotalCount / exportSize;
	    }else{
	      maxNumber = pageTotalCount / exportSize + 1;
	    }
	    
	    if(maxNumber <= 500){
	    	model.addAttribute("isMore", "false");
	    }else{
	      model.addAttribute("isMore", "true");
	    }
	    
	    model.addAttribute("exportSize", Integer.valueOf(exportSize));
	    model.addAttribute("maxNumber", Integer.valueOf(maxNumber));
	    
	    return "/evaluation/statistic/exportEvaluationList";
	  }
	
	  /***
	   * 导出综合测评统计
	   * @param model
	   * @param request
	   * @param evaluation
	   * @param response
	   */
	  @RequestMapping({"/evaluation/statistics/opt-query/exportEvaluation"})
	  public void exportEvaluation(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation, HttpServletResponse response){
		  String exportSize = request.getParameter("evaluationQuery_exportSize");
		    String exportPage = request.getParameter("evaluationQuery_exportPage");
		    Page page = this.evaluationStatisticService.statisticEvaluationPage(Integer.parseInt(exportPage), Integer.parseInt(exportSize), evaluation, request);
			//Page page = this.evaluationStatisticService.statisticEvaluationPage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation, request);

		    List listMap = new ArrayList();
		    List<Object[]> evaluationList = (List)page.getResult();
		    for(Object[] object : evaluationList) {
		    	Map map = new HashMap();
		    	map.put("studentNum", object[0].toString());
		    	map.put("college", object[1].toString());
		    	map.put("major", object[2].toString());
		    	map.put("class", object[3].toString());
		    	map.put("student", object[4].toString());
		    	map.put("moralScoreSum", object[5].toString());
		    	map.put("intellectScoreSum", object[6].toString());
		    	map.put("cultrueScoreSum", object[7].toString());
		    	map.put("capacityScoreSum", object[8].toString());
		    	map.put("scoreSum", object[9].toString());
		    	map.put("year", object[10].toString());
		    	listMap.add(map);
		    }
		    
		    HSSFWorkbook wb;
			try {
				wb = this.excelService.exportData("export_evaluation.xls", "exportEvaluation", listMap);
				String filename = "测评统计表" + exportPage + ".xls";
			    response.setContentType("application/x-excel");
			    response.setHeader("Content-disposition", "attachment;filename=" + new String(filename.getBytes("GBK"), "iso-8859-1"));
			    response.setCharacterEncoding("UTF-8");
			    OutputStream ouputStream = response.getOutputStream();
			    wb.write(ouputStream);
			    ouputStream.flush();
			    ouputStream.close();
			} catch (ExcelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }

}
