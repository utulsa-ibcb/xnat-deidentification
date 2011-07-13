package org.ibcb.xnat.redaction.interfaces;

import java.util.LinkedList;

import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class XNATResultSet {
	protected String type; 
	
	
	public class Row{
		LinkedList<String> values = new LinkedList<String>();
		
		public String getValue(String col){
			int index = columns.indexOf(col);
			
			return values.get(index);
		}
		
		public boolean complete(){
			return values.size()==columns.size();
		}
	}
	
	LinkedList<String> columns;
	LinkedList<Row> rows;
	
	public XNATResultSet(){
		columns = new LinkedList<String>();
		rows = new LinkedList<Row>();
	}
	
	public LinkedList<Row> getRows(){
		return rows;
	}
	
	public String toString(){
		String resultset = "ResultSet records:" + rows.size() + "\n";
		
		for(String c : columns){
			resultset += c + " ";
		}
		
		resultset+="\n";
		
		for(Row r : rows){
			for(String v : r.values){
				resultset += v + " ";
			}
			resultset+="\n";
		}
		
		return resultset;
	}
	
	public static XNATResultSet parseXML(DOMParser parser){
		XNATResultSet set = new XNATResultSet();
		
		Node columns = parser.getDocument().getElementsByTagName("columns").item(0);
		Node rows = parser.getDocument().getElementsByTagName("rows").item(0);
		
		for(int s = 0; s < columns.getChildNodes().getLength(); s++){
			Node col = columns.getChildNodes().item(s);
			
			if(col.getLocalName() != null && col.getLocalName().equalsIgnoreCase("column"))
				set.columns.add(col.getTextContent());
		}
		
		for(int s = 0; s < rows.getChildNodes().getLength(); s++){
			Node row = rows.getChildNodes().item(s);
			
			
			if(row.getLocalName() != null && row.getLocalName().equalsIgnoreCase("row")){
				Row r = set.new Row();
				
				for(int t = 0; t < row.getChildNodes().getLength(); t++){
					Node cell = row.getChildNodes().item(t);
					r.values.add(cell.getTextContent());
				}
				
				if(r.complete())
					set.rows.add(r);
			}
		}
		return set;
	}
	
	
	public void parseXMLResult(DOMParser parser){
		XNATResultSet set = this;
		Node columns = parser.getDocument().getElementsByTagName("columns").item(0);
		Node rows = parser.getDocument().getElementsByTagName("rows").item(0);
		
		for(int s = 0; s < columns.getChildNodes().getLength(); s++){
			Node col = columns.getChildNodes().item(s);
			
			if(col.getLocalName() != null && col.getLocalName().equalsIgnoreCase("column"))
				set.columns.add(col.getTextContent());
		}
		
		for(int s = 0; s < rows.getChildNodes().getLength(); s++){
			Node row = rows.getChildNodes().item(s);
			
			
			if(row.getLocalName() != null && row.getLocalName().equalsIgnoreCase("row")){
				Row r = set.new Row();
				
				for(int t = 0; t < row.getChildNodes().getLength(); t++){
					Node cell = row.getChildNodes().item(t);
					r.values.add(cell.getTextContent());
				}
				
				if(r.complete())
					set.rows.add(r);
			}
		}
	}
}
