package org.prabu.spring.excel.objectbinding.utils;

import static org.prabu.spring.excel.objectbinding.common.ApplicationConstants.DECIMAL;
import static org.prabu.spring.excel.objectbinding.common.ApplicationConstants.INVALID_DATE_VALUE;
import static org.prabu.spring.excel.objectbinding.common.ApplicationConstants.INVALID_DECIMAL_VALUE;
import static org.prabu.spring.excel.objectbinding.common.ApplicationConstants.INVALID_STRING_VALUE;
import static org.prabu.spring.excel.objectbinding.common.ApplicationConstants.TIMESTAMP;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.prabu.spring.excel.objectbinding.domain.Base;
import org.prabu.spring.excel.objectbinding.handler.ColumnTemplate;
import org.prabu.spring.excel.objectbinding.handler.FileTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;


public class ExcelUtility {

	private final static Logger log = LoggerFactory
			.getLogger(ExcelUtility.class);

	public static <T extends Base>  List<T> readXlFile(Workbook workbook,
			FileTemplate fileTemplate, Class<T> clazz){
		return readXlFile(workbook, fileTemplate, clazz, true);
	}
	public static <T extends Base>  List<T> readXlFile(Workbook workbook,
			FileTemplate fileTemplate, Class<T> clazz, boolean hasHeaderRow) {
		List<T> objList = null;
		BindException bindException;
		T obj ;
		ColumnTemplate column;

		try {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			objList = new ArrayList<T>();

			while (rowIterator.hasNext()) {
				obj = clazz.newInstance();
				
				Row row = (Row) rowIterator.next();
				bindException = new BindException(obj, ""+ row.getRowNum());

				if (hasHeaderRow && row.getRowNum() < 1) {
					continue; // just skip the row if row number 0  if header row is true
				}
				

					for (Integer pos : fileTemplate.getColumnTemplatesMap()
							.keySet()) {
						column = fileTemplate.getColumnTemplateByPos(pos);
						switch (column.getType()) {
						case TIMESTAMP:
							BeanUtils.setProperty(
									obj,
									column.getBeanColumnName(),
									getDateCellValue(row.getCell(pos),
											bindException, column));
							break;
						case DECIMAL:
							BeanUtils.setProperty(
									obj,
									column.getBeanColumnName(),
									getDecimalCellValue(row.getCell(pos),
											bindException, column));
							break;

						default:
							BeanUtils.setProperty(
									obj,
									column.getBeanColumnName(),
									getStringCellValue(row.getCell(pos),
											bindException, column));
							break;
						}
					}
				
				obj.setErrors(bindException);
				obj.setRow(row.getRowNum()+1);
				objList.add(obj);
			}
		} catch (Exception e) {
			log.error("File contain some invalid values" + e.getMessage());
			e.printStackTrace();
		}

		return objList;
	}
	
	private static Date getDateCellValue(Cell cell ,
			Errors result, ColumnTemplate columnTemplate) {
		Date parsedDate = null;
		if(cell==null){
			return parsedDate;
		}
		try {
		parsedDate = cell.getDateCellValue();
		} catch (IllegalStateException | NumberFormatException e ) {
			log.error(e.getMessage());
			result.rejectValue(columnTemplate.getBeanColumnName(),
					INVALID_DATE_VALUE, cell.toString());
		}
		return parsedDate;
	}

	private static String getStringCellValue(Cell cell, Errors errors,
			ColumnTemplate columnTemplate) {

		String rtrnVal = "";
		if(cell==null){
			return rtrnVal;
		}
		try {

			if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
				rtrnVal = "" + cell.getNumericCellValue();
			} else {
				rtrnVal = cell.getStringCellValue();
			}
		} catch (Exception exception) {
			log.error(exception.getMessage());
			exception.printStackTrace();
			errors.rejectValue(columnTemplate.getBeanColumnName(),
					INVALID_STRING_VALUE, cell.toString());
		}
		return rtrnVal;
	}

	private static BigDecimal getDecimalCellValue(Cell cell, Errors errors,
			ColumnTemplate columnTemplate) {
		BigDecimal rtrnVal = new BigDecimal(0);
		if(cell==null){
			return rtrnVal;
		}
		try {
			if (cell != null) {
				if (cell.toString().trim().length() == 0)
					return rtrnVal = new BigDecimal(0);
				else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
					rtrnVal = BigDecimal.valueOf(cell.getNumericCellValue());
					return rtrnVal = rtrnVal.setScale(3,
							BigDecimal.ROUND_CEILING).stripTrailingZeros();
				}
			}
		} catch (Exception exception) {
			log.error(exception.getMessage());
			errors.rejectValue(columnTemplate.getBeanColumnName(),
					INVALID_DECIMAL_VALUE, cell.toString());
		}

		return rtrnVal;
	}
}
