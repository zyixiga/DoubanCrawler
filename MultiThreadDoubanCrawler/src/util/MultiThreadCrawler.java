package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import doubanBook.DoubanBookInfo;

public class MultiThreadCrawler {
	private List<String> bookLinks; // book url list
	private List<DoubanBookInfo> bookInfos; // book info list
	private int total; // book 总数
	private int count; // 找到的book数量
	private int threadCount;
	private int maxThreadNum = 30;

	public List<String> begin(String url) {
		beginAddBookUrl(url);
		while (bookLinks.size() < total && count != threadCount) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("finished book url crawling...");
		System.out.println("start to book info crawling");
		beginAddBookInfo();
		while (bookLinks.size() > 0) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// sort DoubanBook
		Collections.sort(bookInfos, new Comparator<DoubanBookInfo>() {
			@Override
			public int compare(DoubanBookInfo dbi1, DoubanBookInfo dbi2) {
				int largerthan2000 = 0;
				if (dbi1.getVotes() >= 2000 && dbi2.getVotes() < 2000)
					largerthan2000 = -1;
				else if (dbi1.getVotes() < 2000 && dbi2.getVotes() >= 2000)
					largerthan2000 = 1;
				int rateCompare = (largerthan2000 != 0 ? largerthan2000
						: (int) (dbi2.getRate() * 10 - dbi1.getRate() * 10));
				int voteCompare = rateCompare != 0 ? rateCompare : dbi2.getVotes() - dbi1.getVotes();
				return voteCompare;
			}
		});

		List<String> dataList = new ArrayList<String>();
		int minLen = Math.min(100, bookInfos.size());
		for (int i = 0; i < minLen; i++)
			dataList.add(bookInfos.get(i).toString());
		return dataList;
	}

	// 开始爬取book url
	public void beginAddBookUrl(String url) {
		String pageUrl = url + "&start=0";
		String pageContent = SendGet(url);
		String temp = findPattern("共(.+?)<", pageContent).trim();
		total = temp.equals("") ? 0 : Integer.parseInt(temp);
		bookLinks = new ArrayList<String>();
		threadCount = Math.min(maxThreadNum, getPageCount(total));
		count = 0;
		for (int i = 0; i < threadCount; i++) {
			String startUrl = nextUrl(pageUrl, i);
			new Thread(new Runnable() {
				@Override
				public void run() {
					String startUrl = Thread.currentThread().getName();
					String pageContent = SendGet(startUrl);
					String threadUrl = startUrl;
					System.out.println(startUrl);
					// while has next page
					while (pageContent != null && pageContent != "") {
						Set<String> bookUrlsInCurrentPage = getBookLink(pageContent);
						addBookUrl(bookUrlsInCurrentPage);
						System.out.println(bookLinks.size());
						// go to next page
						threadUrl = nextUrl(threadUrl, threadCount);
						if (threadUrl == "")
							break;
						// get page content
						pageContent = SendGet(threadUrl);
					}
					synchronized (this) {
						count++;
					}
				}
			}, startUrl).start();
		}
	}

	public void beginAddBookInfo() {
		bookInfos = new ArrayList<DoubanBookInfo>();
		count = 0;
		for (int i = 0; i < maxThreadNum; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					while (!bookLinks.isEmpty()) {
						DoubanBookInfo book = getBookInfo(getBookUrl());
						if (book != null)
							addBookInfo(book);
					}
				}
			}).start();
		}
	}

	// if next page do not exist, return nothing
	private String nextUrl(String url, int step) {
		int split = url.lastIndexOf('=');
		int count = Integer.parseInt(url.substring(split + 1));
		StringBuffer nextUrl = new StringBuffer();
		int nextStart = count + step * 15;
		if (nextStart > total)
			return "";
		nextUrl.append(url.substring(0, split + 1)).append(nextStart);
		return nextUrl.toString();
	}

	private String findPattern(String regex, String content) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		return matcher.find() ? matcher.group(1) : "";
	}

	// 提取全部页面信息
	public String SendGet(String url) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (url == null || url == "")
			return "";
		// 定义一个字符串用来存储网页内容
		String result = "";
		// 定义一个缓冲字符输入流
		BufferedReader in = null;
		try {
			// 将string转成url对象
			URL realUrl = new URL(url);
			// 初始化一个链接到那个url的连接
			URLConnection connection = realUrl.openConnection();
			// 以防被禁止抓取
			connection.setRequestProperty("User-Agent",
					"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
			// 开始实际的连接
			connection.connect();
			// 初始化 BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			// 用来临时存储抓取到的每一行的数据
			String line;
			while ((line = in.readLine()) != null) {
				// 遍历抓取到的每一行并将其存储到result里面
				result += line;
			}
		} catch (Exception e) {
			System.out.println("发送GET请求出现异常！" + e);
			e.printStackTrace();
		}
		// 使用finally来关闭输入流
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}

	// 提取当前页面所有书的书名和链接
	private Set<String> getBookLink(String content) {
		// 预定义一个Set来存储结果
		Set<String> bookLinks = new HashSet<String>();
		// 用来匹配url，也就是问题的链接
		Pattern urlPattern = Pattern.compile("nbg.+?href=\"(.+?)\"");
		Matcher urlMatcher = urlPattern.matcher(content);
		boolean isFind = urlMatcher.find();
		while (isFind) {
			// 添加成功匹配的结果
			bookLinks.add(urlMatcher.group(1));
			// 继续查找下一个匹配对象
			isFind = urlMatcher.find();
		}
		return bookLinks;
	}

	// 从列表里提取book的url
	private synchronized String getBookUrl() {
		if (bookLinks.isEmpty())
			return "";
		String bookUrl;
		bookUrl = bookLinks.get(0);
		bookLinks.remove(0);
		return bookUrl;
	}

	private synchronized void addBookUrl(Set<String> urls) {
		bookLinks.addAll(urls);
	}

	private synchronized void addBookInfo(DoubanBookInfo book) {
		bookInfos.add(book);
	}

	// 提取当前页面所有书的数据
	public DoubanBookInfo getBookInfo(String url) {
		String content = SendGet(url);
		// 用来匹配作者
		Pattern titlePattern = Pattern.compile("nbg.+?alt=\"(.+?)\"");
		Matcher titleMatcher = titlePattern.matcher(content);
		// 用来匹配作者
		Pattern authorPattern = Pattern.compile("href=\"/search.+?>(.+?)<");
		Matcher authorMatcher = authorPattern.matcher(content);
		// 用来匹配出版社
		Pattern publisherPattern = Pattern.compile("出版社:</span>(.+?)<");
		Matcher publisherMatcher = publisherPattern.matcher(content);
		// 用来匹配副标题
		Pattern subTitlePattern = Pattern.compile("副标题:</span>(.+?)<");
		Matcher subTitleMatcher = subTitlePattern.matcher(content);
		// 用来匹配原作名
		Pattern originalNamePattern = Pattern.compile("原作名:</span>(.+?)<");
		Matcher originalNameMatcher = originalNamePattern.matcher(content);
		// 用来匹配译者
		Pattern translaterPattern = Pattern.compile("译者.+?href.+?>(.+?)<");
		Matcher translaterMatcher = translaterPattern.matcher(content);
		// 用来匹配出版年
		Pattern publishYearPattern = Pattern.compile("出版年:</span>(.+?)<");
		Matcher publishYearMatcher = publishYearPattern.matcher(content);
		// 用来匹配页数
		Pattern pagePattern = Pattern.compile("页数:</span>(.+?)<");
		Matcher pageMatcher = pagePattern.matcher(content);
		// 用来匹配定价
		Pattern pricePattern = Pattern.compile("定价:</span>(.+?)<");
		Matcher priceMatcher = pricePattern.matcher(content);
		// 用来匹配装帧
		Pattern bookBindingPattern = Pattern.compile("装帧:</span>(.+?)<");
		Matcher bookBindingNameMatcher = bookBindingPattern.matcher(content);
		// 用来匹配丛书
		Pattern seriesPattern = Pattern.compile("丛书.+?href.+?>(.+?)<");
		Matcher seriesMatcher = seriesPattern.matcher(content);
		// 用来匹配ISBN
		Pattern ISBNPattern = Pattern.compile("ISBN:</span>(.+?)<");
		Matcher ISBNMatcher = ISBNPattern.matcher(content);
		// 用来匹配丛书
		Pattern ratePattern = Pattern.compile("average\">(.+?)<");
		Matcher rateMatcher = ratePattern.matcher(content);
		// 用来匹配ISBN
		Pattern votesPattern = Pattern.compile("votes\">(.+?)<");
		Matcher votesMatcher = votesPattern.matcher(content);

		// 定义一个豆瓣读书对象来存储抓取到的信息
		// main info
		if (titleMatcher.find() && ISBNMatcher.find() && rateMatcher.find()) {
			DoubanBookInfo book = new DoubanBookInfo();
			book.setTitle(titleMatcher.group(1).trim());
			book.setISBN(ISBNMatcher.group(1).trim());
			String rate = rateMatcher.group(1).trim();
			if (!rate.equals(""))
				book.setRate(Double.parseDouble(rate));
			else
				book.setRate(0);
			if (votesMatcher.find())
				book.setVotes(Integer.parseInt(votesMatcher.group(1)));
			else
				book.setVotes(0);
			// optional info
			if (authorMatcher.find())
				book.setSubTitle(authorMatcher.group(1).trim());
			if (publisherMatcher.find())
				book.setPublisher(publisherMatcher.group(1).trim());
			if (publishYearMatcher.find())
				book.setPublishYear(publishYearMatcher.group(1).trim());
			if (priceMatcher.find())
				book.setPrice(priceMatcher.group(1).trim());
			if (bookBindingNameMatcher.find())
				book.setBookBinding(bookBindingNameMatcher.group(1).trim());
			if (subTitleMatcher.find())
				book.setSubTitle(subTitleMatcher.group(1).trim());
			if (originalNameMatcher.find())
				book.setOriginalName(originalNameMatcher.group(1).trim());
			if (translaterMatcher.find())
				book.setTranslater(translaterMatcher.group(1).trim());
			if (seriesMatcher.find())
				book.setSeries(seriesMatcher.group(1).trim());
			if (pageMatcher.find())
				book.setPages(pageMatcher.group(1).trim());

			return book;
		}
		return null;
	}

	private int getPageCount(int recordCount) {
		int pageSize = 15;
		int PageCount = recordCount % pageSize == 0 ? recordCount / pageSize : recordCount / pageSize + 1;
		if (PageCount < 1)
			PageCount = 1;
		return PageCount;
	}
}
