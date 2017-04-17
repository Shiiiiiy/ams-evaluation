package com.uws.evaluation.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.uws.common.service.IStuJobTeamSetCommonService;
import com.uws.core.util.DataUtil;
import com.uws.domain.base.BaseAcademyModel;

public class EvaluationCommon {
	
	/**
	 * 获取查询条件
	 * @param monthId字符串
	 * @return in(......)查询条件
	 */
	public String getCondition(String Ids) {
		StringBuffer sbff = new StringBuffer();
		if(DataUtil.isNotNull(Ids)){
			sbff.append(" (");
			String stuArray[] = Ids.split(",");
			for(int i=0;i<stuArray.length;i++){
				String stuId = stuArray[i];
				if(stuArray.length-1==i){
					sbff.append("'"+stuId+"'");
				}else{
					sbff.append("'"+stuId+"'").append(",");
				}
			}
			sbff.append(")");
		}
		return sbff.toString();
	}

}
