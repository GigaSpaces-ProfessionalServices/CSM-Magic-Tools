package com.gs.csm.data;

import com.gigaspaces.annotation.pojo.SpaceId;

public class StocksHistoriesPojo {
		private String date = "date";
		private String volume = "volume";
		private String open = "open";
		private String high = "high";
		private String low = "low";
		private String close = "close";
		private String adjclose = "adjclose";
		private String symbol = "symbol";
		private String Id = "Id";

		public String  getDate(){
			return this.date;
		}
		public void  setDate(String date){
			 this.date = date;
		}

		public String  getVolume(){
			return this.volume;
		}
		public void  setVolume(String volume){
			 this.volume = volume;
		}

		public String  getOpen(){
			return this.open;
		}
		public void  setOpen(String open){
			 this.open = open;
		}

		public String  getHigh(){
			return this.high;
		}
		public void  setHigh(String high){
			 this.high = high;
		}

		public String  getLow(){
			return this.low;
		}
		public void  setLow(String low){
			 this.low = low;
		}

		public String  getClose(){
			return this.close;
		}
		public void  setClose(String close){
			 this.close = close;
		}

		public String  getAdjclose(){
			return this.adjclose;
		}
		public void  setAdjclose(String adjclose){
			 this.adjclose = adjclose;
		}

		public String  getSymbol(){
			return this.symbol;
		}
		public void  setSymbol(String symbol){
			 this.symbol = symbol;
		}

		@SpaceId
		public String  getId(){
			return this.Id;
		}
		public void  setId(String Id){
			 this.Id = Id;
		}

// toString() Method
		 public String toString(){
			 return "{\"date\"="+date+",\"volume\"="+volume+",\"open\"="+open+",\"high\"="+high+",\"low\"="+low+",\"close\"="+close+",\"adjclose\"="+adjclose+",\"symbol\"="+symbol+",\"Id\"="+Id+"}";
		}
}
