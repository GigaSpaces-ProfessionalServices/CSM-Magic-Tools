package com.gs.csm.data;

import com.gigaspaces.annotation.pojo.SpaceId;

public class MyNewPojo {
		private String Id = "Id";
		private String Date = "Date";
		private String Open = "Open";
		private String High = "High";
		private String Low = "Low";
		private String Close = "Close";
		private String Volume = "Volume";
		private String OpenInt = "OpenInt";

		@SpaceId (autoGenerate = true)
		public String  getId(){
			return this.Id;
		}
		public void  setId(String Id){
			 this.Id = Id;
		}

		public String  getDate(){
			return this.Date;
		}
		public void  setDate(String Date){
			 this.Date = Date;
		}

		public String  getOpen(){
			return this.Open;
		}
		public void  setOpen(String Open){
			 this.Open = Open;
		}

		public String  getHigh(){
			return this.High;
		}
		public void  setHigh(String High){
			 this.High = High;
		}

		public String  getLow(){
			return this.Low;
		}
		public void  setLow(String Low){
			 this.Low = Low;
		}

		public String  getClose(){
			return this.Close;
		}
		public void  setClose(String Close){
			 this.Close = Close;
		}

		public String  getVolume(){
			return this.Volume;
		}
		public void  setVolume(String Volume){
			 this.Volume = Volume;
		}

		public String  getOpenInt(){
			return this.OpenInt;
		}
		public void  setOpenInt(String OpenInt){
			 this.OpenInt = OpenInt;
		}

// toString() Method
		 public String toString(){
			 return "{\"Date\"="+Date+",\"Open\"="+Open+",\"High\"="+High+",\"Low\"="+Low+",\"Close\"="+Close+",\"Volume\"="+Volume+",\"OpenInt\"="+OpenInt+"}";
		}
}
