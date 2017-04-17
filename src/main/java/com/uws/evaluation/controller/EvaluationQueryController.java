package com.uws.evaluation.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
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
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;
import com.uws.domain.base.BaseClassModel;
import com.uws.domain.base.BaseMajorModel;
import com.uws.domain.evaluation.EvaluationDetail;
import com.uws.domain.evaluation.EvaluationInfo;
import com.uws.domain.evaluation.EvaluationInfoVo;
import com.uws.evaluation.service.IEvaluationQueryService;
import com.uws.evaluation.service.IStuEvaluationService;
import com.uws.log.Logger;
import com.uws.log.LoggerFactory;
import com.uws.sys.model.Dic;
import com.uws.sys.service.DicUtil;
import com.uws.sys.service.impl.DicFactory;
import com.uws.util.ProjectSessionUtils;

@Controller
public class EvaluationQueryController extends BaseController{
	
	//log
	private Logger log=new LoggerFactory(EvaluationQueryController.class);
		
	@Autowired
	private IEvaluationQueryService evaluationQueryService;
	
	@Autowired
	private IStuEvaluationService stuEvaluationservice;
	
	@Autowired
	private IBaseDataService baseDataService;
	
	@Autowired
	private ICompService compService;
	
	@Autowired
	private IExcelService excelService;
	
	//字典工具类
	private DicUtil dicUtil=DicFactory.getDicUtil();
	
	/****
	 * 个人综合测评查询
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping("/evaluation/student/opt-query/queryStudentEvaluationList")
	public String queryEvaluationPage(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation){
		log.info("查询个人综合测评!");
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
		if(null == evaluation.getYearId()){//默认当前学年
			Dic yearDic = SchoolYearUtil.getYearDic();
			evaluation.setYearId(yearDic.getId());
		}
		if(null == evaluation.getTermId()){//默认当前学期
			Dic termDic = SchoolYearUtil.getCurrentTermDic();
			evaluation.setTermId(termDic.getId());
		}
		
		Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
		Page page = this.evaluationQueryService.queryEvaluationPage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation, request);
		
		boolean flag = ProjectSessionUtils.checkIsStudent(request);//判断是否是学生
		model.addAttribute("flag", flag);
		
		model.addAttribute("collageList", collageList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("schoolYearList", schoolYearList);
		model.addAttribute("termList", termList);
		model.addAttribute("monthList", monthList);
		model.addAttribute("page", page);

		return "/evaluation/queryEvaluation/studentEvaluationList";
	}
	
	/**
	 * 查看测评明细
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping({"/evaluation/student/opt-query/queryEvaluationDetail"})
	public String queryEvaluationDetail(ModelMap model, String id){
		String yearId="";
		String termId="";
		String monthId="";
		EvaluationInfo evaluation=new EvaluationInfo();
		if(DataUtil.isNotNull(id)){
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
		
		return "/evaluation/queryEvaluation/viewEvaluationDetail";
	}
	
	/****
	 * 班级综合测评查询
	 * @param model
	 * @param request
	 * @return
	 */
	@RequestMapping("/evaluation/class/opt-query/queryClassEvaluationList")
	public String queryClassEvaluationPage(ModelMap model, HttpServletRequest request, EvaluationInfo evaluation){
		log.info("查询个人综合测评!");
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
		
		Dic yearDic = SchoolYearUtil.getYearDic();
		if(null == evaluation.getYearId()){//默认当前学年
			evaluation.setYearId(yearDic.getId());
		}
		
		Integer pageNo = request.getParameter("pageNo")!=null?Integer.valueOf(request.getParameter("pageNo")):1;
		Page page = this.evaluationQueryService.queryClassEvaluationPage(pageNo, Page.DEFAULT_PAGE_SIZE, evaluation, request);
		
		model.addAttribute("collageList", collageList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("classList", classList);
		model.addAttribute("evaluation", evaluation);
		model.addAttribute("schoolYearList", schoolYearList);
		model.addAttribute("termList", termList);
		model.addAttribute("monthList", monthList);
		model.addAttribute("page", page);

		return "/evaluation/queryEvaluation/classEvaluationList";
	}
	
	/**
	 * 查看班级测评明细
	 * @param model
	 * @param id
	 * @return
	 */
	@RequestMapping({"/evaluation/class/opt-query/queryClassEvaluationDetail.do"})
	public String queryClassEvaluationDetail(ModelMap model, String id){
		String yearId="";
		String termId="";
		String monthId="";
		EvaluationInfo evaluation=new EvaluationInfo();
		if(DataUtil.isNotNull(id)){
			evaluation=this.stuEvaluationservice.getEvaluationInfoById(id);
		}
		
		//获取测评明细列表
		if(DataUtil.isNotNull(evaluation)){
			yearId=evaluation.getYear().getId();
			termId=evaluation.getTerm().getId();
			monthId=evaluation.getMonth().getId();
			this.evaluationQueryService.getClassEvaluationDetail(evaluation, model);
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
		model.addAttribute("evaluation", evaluation);
		
		return "/evaluation/queryEvaluation/viewClassEvaluationDetail";
	}
	
	 /***
	   * 导出综合测评统计
	   * @param model
	   * @param request
	   * @param evaluation
	   * @param response
	   */
	  @RequestMapping({"/evaluation/monthQuery/query/exportClassEvaluation"})
	  public void exportClassEvaluation(String id, HttpServletResponse response){
		    EvaluationInfo evaluation=new EvaluationInfo();
			if(DataUtil.isNotNull(id)){
				evaluation=this.stuEvaluationservice.getEvaluationInfoById(id);
				HSSFWorkbook wb = new HSSFWorkbook();
				//定义sheet页名称
	            HSSFSheet sheet = wb.createSheet("sheet1");
				//设定样式：居中显示
				//标题单元格样式
				HSSFCellStyle titleStyle = (HSSFCellStyle) wb.createCellStyle();
				titleStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);//水平居中
				titleStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);//垂直居中  
				HSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 18);
				//字号
				f.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
				sheet.setColumnWidth(0, 20 * 256);  
				sheet.setColumnWidth(1, 30 * 256);  
				sheet.setColumnWidth(2, 30 * 256);  
				sheet.setColumnWidth(3, 30 * 256);  
				sheet.setColumnWidth(4, 30 * 256);  
				sheet.setColumnWidth(5, 30 * 256);  
				//加粗 
				titleStyle.setFont(f); 
				
				//内容单元格样式
				HSSFCellStyle contentStyle = (HSSFCellStyle) wb.createCellStyle();
				contentStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT);//水平居左
				contentStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);//垂直居中  
				HSSFFont contentf = wb.createFont(); 
				contentf.setFontHeightInPoints((short) 16);
				contentf.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);//字号
				contentStyle.setFont(contentf); //加粗 
				
				//内容样式
				HSSFCellStyle contentCss = (HSSFCellStyle) wb.createCellStyle();
				contentCss.setAlignment(HSSFCellStyle.ALIGN_LEFT);//水平居左
				contentCss.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);//垂直居中  
				HSSFFont contentCssf = wb.createFont(); 
				contentCssf.setFontHeightInPoints((short) 12);
				contentCssf.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);//字号
				
				//第一行 标题行
				HSSFRow row1 = sheet.createRow(0);  
				HSSFCell cell0=row1.createCell(0);
				row1.setHeightInPoints(35);
				sheet.addMergedRegion(new CellRangeAddress(0,0,0,5));
				//cell0.setCellValue(evaluation.getYear().getName() + evaluation.getTerm().getName() + evaluation.getMonth().getName() +"综合素质测评月记录表");
				cell0.setCellValue("综合素质测评月记录表(" + evaluation.getMonth().getName() + ")");
				cell0.setCellStyle(titleStyle);
				//第二行 字段行
				HSSFRow row2 = sheet.createRow(1); 
				row2.setHeightInPoints(35);
				sheet.addMergedRegion(new CellRangeAddress(1,1,0,1));
				HSSFCell cell1 = row2.createCell(0);
				cell1.setCellValue("学院:" + evaluation.getStudent().getCollege().getName());
				cell1.setCellStyle(contentStyle);
				
				cell1 = row2.createCell(1);
				cell1.setCellStyle(contentStyle);
				
				cell1 = row2.createCell(2);
				sheet.addMergedRegion(new CellRangeAddress(1,1,2,3));
				cell1.setCellValue("班级:" + evaluation.getStudent().getClassId().getClassName());
				cell1.setCellStyle(contentStyle);
				
				cell1 = row2.createCell(3);
				cell1.setCellStyle(contentStyle);
				
				cell1 = row2.createCell(4);
				sheet.addMergedRegion(new CellRangeAddress(1,1,4,5));
				cell1.setCellValue("负责人签字:");
				cell1.setCellStyle(contentStyle);
				
				cell1 = row2.createCell(5);
				cell1.setCellStyle(contentStyle);
				
				//第三行 字段行
				HSSFRow row3 = sheet.createRow(2); 
				row3.setHeightInPoints(35);
				sheet.addMergedRegion(new CellRangeAddress(2,2,0,1));
				HSSFCell cell2 = row3.createCell(0);
				cell2.setCellValue("班主任签字:");
				cell2.setCellStyle(contentStyle);
				
				cell2 = row2.createCell(1);
				cell2.setCellStyle(contentStyle);
				
				cell2 = row3.createCell(2);
				sheet.addMergedRegion(new CellRangeAddress(2,2,2,5));
				cell2.setCellValue("带班辅导员签字:");
				cell2.setCellStyle(contentStyle);
				
				cell2 = row3.createCell(3);
				cell2.setCellStyle(contentStyle);
				
				cell2 = row3.createCell(4);
				cell2.setCellStyle(contentStyle);
				
				cell2 = row3.createCell(5);
				cell2.setCellStyle(contentStyle);
				
				//第四行 表头
				HSSFRow row4 = sheet.createRow(3); 
				row4.setHeightInPoints(35);
				
				HSSFCell cell3 = row4.createCell(0);
				cell3.setCellValue("序号");
				cell3.setCellStyle(contentStyle);
				
				cell3 = row4.createCell(1);
				cell3.setCellValue("姓名");
				cell3.setCellStyle(contentStyle);
				
				cell3 = row4.createCell(2);
				cell3.setCellValue("类型");
				cell3.setCellStyle(contentStyle);
				
				cell3 = row4.createCell(3);
				cell3.setCellValue("加减分事由");
				cell3.setCellStyle(contentStyle);
				
				cell3 = row4.createCell(4);
				cell3.setCellValue("加减分");
				cell3.setCellStyle(contentStyle);
				
				cell3 = row4.createCell(5);
				cell3.setCellValue("确认签字");
				cell3.setCellStyle(contentStyle);
				//查询班级单月的测评
				List<EvaluationInfoVo> list = this.evaluationQueryService.queryClassEvaluationList(evaluation);
				Dic moralDic = this.dicUtil.getDicInfo("EVALUATION_BASE_TYPE", "MORAL");//德育字典
				Dic cultureDic = this.dicUtil.getDicInfo("EVALUATION_BASE_TYPE", "CULTURE");//文体字典
				Dic capacityDic = this.dicUtil.getDicInfo("EVALUATION_BASE_TYPE", "CAPACITY");//能力
				//接下来是赋值
				for (int i = 0,j = 0; i < list.size(); i++, j += 3){
					EvaluationInfoVo eval = list.get(i);
					if(DataUtil.isNotNull(eval)){
						Map<String,String> detailMap = this.evaluationQueryService.queryMonthEvaluationDetail(eval.getId());
						//序号
						HSSFRow row_j = sheet.createRow(j+4);
						HSSFCell cell = row_j.createCell(0);
						cell.setCellValue(i+1);
						sheet.addMergedRegion(new CellRangeAddress(j+4,j+6,0,0));
						cell.setCellStyle(contentCss);
						//姓名
						cell = row_j.createCell(1);
						cell.setCellValue(list.get(i).getStudent().getName());
						sheet.addMergedRegion(new CellRangeAddress(j+4,j+6,1,1));
						cell.setCellStyle(contentCss);
						//德育类别
						cell = row_j.createCell(2);
						cell.setCellValue("德育");
						cell.setCellStyle(contentCss);
						//详细
						cell = row_j.createCell(3);
						cell.setCellValue(detailMap.get(moralDic.getId()));
						cell.setCellStyle(contentCss);
						//分数
						cell = row_j.createCell(4);
						cell.setCellValue(list.get(i).getMoralScoreSum());
						cell.setCellStyle(contentCss);
						//确认
						cell = row_j.createCell(5);
						sheet.addMergedRegion(new CellRangeAddress(j+4,j+6,5,5));
						cell.setCellStyle(contentCss);
						
						//文体类别
						row_j = sheet.createRow(j+5);
						cell = row_j.createCell(2);
						cell.setCellValue("文体");
						cell.setCellStyle(contentCss);
						//详细
						cell = row_j.createCell(3);
						cell.setCellValue(detailMap.get(cultureDic.getId()));
						cell.setCellStyle(contentCss);
						//分数
						cell = row_j.createCell(4);
						cell.setCellValue(list.get(i).getCultrueScoreSum());
						cell.setCellStyle(contentCss);
						
						//能力类别
						row_j = sheet.createRow(j+6);
						cell = row_j.createCell(2);
						cell.setCellValue("能力");
						cell.setCellStyle(contentCss);
						//详细
						cell = row_j.createCell(3);
						cell.setCellValue(detailMap.get(capacityDic.getId()));
						cell.setCellStyle(contentCss);
						//分数
						cell = row_j.createCell(4);
						cell.setCellValue(list.get(i).getCapacityScoreSum());
						cell.setCellStyle(contentCss);
					}
	            }
				
				//String filename = evaluation.getYear().getName() + evaluation.getTerm().getName()
				//		 + evaluation.getMonth().getName() + evaluation.getStudent().getCollege().getName()
				//		 + evaluation.getStudent().getMajor().getMajorName()+ evaluation.getStudent().getClassId().getClassName()
				//		 + "综合素质测评月记录表" + ".xls";
				String filename = evaluation.getStudent().getClassId().getClassName()
						+ "综合素质测评月记录表" + evaluation.getMonth().getName() + ".xls";
			    try {
			    	response.setContentType("application/x-excel");
					response.setHeader("Content-disposition", "attachment;filename=" + new String(filename.getBytes("GBK"), "iso-8859-1"));
					response.setCharacterEncoding("UTF-8");
					ServletOutputStream fos = response.getOutputStream();
					wb.write(fos);
					fos.close();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
	  }
}
