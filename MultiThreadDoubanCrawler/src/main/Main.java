package main;

import java.util.List;

import util.CSVUtils;
import util.MultiThreadCrawler;

public class Main {

	public static void main(String[] args) {

//		String url = "https://book.douban.com/subject_search?search_text=%E4%BA%92%E8%81%94%E7%BD%91%EF%BC%8C%E7%BC%96%E7%A8%8B%EF%BC%8C%E7%AE%97%E6%B3%95&cat=1001";
//		String url = "https://book.douban.com/subject_search?search_text=%E4%BA%92%E8%81%94%E7%BD%91+%E7%AE%97%E6%B3%95+%E8%BD%AF%E4%BB%B6&cat=1001";
//		String url = "https://book.douban.com/subject_search?search_text=%E7%83%AD%E8%A1%80%E9%AB%98%E6%A0%A1&cat=1001";
		String url = "https://book.douban.com/subject_search?search_text=%E6%98%8E%E5%B0%BC&cat=1001";
		MultiThreadCrawler spider = new MultiThreadCrawler();
		List<String> dataList = spider.begin(url);
		CSVUtils.exportCsv("sorted_list_4.csv", dataList);
	}
}
