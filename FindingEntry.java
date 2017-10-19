package com.bangblue;

/**
 * FindingEntry Java Object
 * This object contain singular lines of the poam and rar, you'll need to set each of the vars for a complete entry
 * @var findingID;
 * @var description;
 * @var control;
 * @var poc;
 * @var cat;
 * @var level;
 * @var scheduledcompletiondate;
 * @var findingSource;
 * @var comment;
 * @var systemName;
 * @var findingDetail;
 * @var fixText;
 * @var checkContent;
 *  
 *  
 * */
public class FindingEntry {

    	private String findingID;
    	private String description;
    	private String control;
    	private String poc;
    	private String cat;
    	private String level;
    	private String scheduledcompletiondate;
    	private String findingSource;
    	private String comment;
    	private String systemName;
    	private String findingDetail;
    	private String fixText;
    	private String checkContent;
	
		public String getCheckContent() {
			return checkContent;
		}
		public void setCheckContent(String checkContent) {
			this.checkContent = checkContent;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}	
		public String getLevel() {
			return level;
		}
		public void setLevel(String level) {
			this.level = level;
		}
		public String getCat() {
			return cat;
		}
		public void setCat(String cat) {
			this.cat = cat;
		}
		public String getControl() {
			return control;
		}
		public void setControl(String iac) {
			this.control = iac;
		}
		public String getPoc() {
			return poc;
		}	
		public String getScheduledcompletiondate() {
			return scheduledcompletiondate;
		}	
		public void setScheduledcompletiondate(String scheduledcompletiondate) {
			this.scheduledcompletiondate = scheduledcompletiondate;
		}
		public String getFindingSource() {
			return findingSource;
		}
		public void setFindingSource(String findingSource) {
			this.findingSource = findingSource;
		}
		public String getFindingID() {
			return findingID;
		}
		public void setFindingID(String findingID) {
			this.findingID = findingID;
		}
		public String getComment() {
			return comment;
		}
		public void setComment(String comment) {
			this.comment = comment;
		}
		public String getSystemName() {
			return systemName;
		}
		public void setSystemName(String systemName) {
			this.systemName = systemName;
		}
		public String getFindingDetail() {
			return findingDetail;
		}
		public void setFindingDetail(String findingDetail) {
			this.findingDetail = findingDetail;
		}
		public String getFixText() {
			return fixText;
		}
		public void setFixText(String fixText) {
			this.fixText = fixText;
		}    		
}

