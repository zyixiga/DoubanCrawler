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
	private int total; // book ����
	private int count; // �ҵ���book����
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

	// ��ʼ��ȡbook url
	public void beginAddBookUrl(String url) {
		String pageUrl = url + "&start=0";
		String pageContent = SendGet(url);
		String temp = findPattern("��(.+?)<", pageContent).trim();
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

	// ��ȡȫ��ҳ����Ϣ
	public String SendGet(String url) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (url == null || url == "")
			return "";
		// ����һ���ַ��������洢��ҳ����
		String result = "";
		// ����һ�������ַ�������
		BufferedReader in = null;
		try {
			// ��stringת��url����
			URL realUrl = new URL(url);
			// ��ʼ��һ�����ӵ��Ǹ�url������
			URLConnection connection = realUrl.openConnection();
			// �Է�����ֹץȡ
			connection.setRequestProperty("User-Agent",
					"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
			// ��ʼʵ�ʵ�����
			connection.connect();
			// ��ʼ�� BufferedReader����������ȡURL����Ӧ
			in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			// ������ʱ�洢ץȡ����ÿһ�е�����
			String line;
			while ((line = in.readLine()) != null) {
				// ����ץȡ����ÿһ�в�����洢��result����
				result += line;
			}
		} catch (Exception e) {
			System.out.println("����GET��������쳣��" + e);
			e.printStackTrace();
		}
		// ʹ��finally���ر�������
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

	// ��ȡ��ǰҳ�������������������
	private Set<String> getBookLink(String content) {
		// Ԥ����һ��Set���洢���
		Set<String> bookLinks = new HashSet<String>();
		// ����ƥ��url��Ҳ�������������
		Pattern urlPattern = Pattern.compile("nbg.+?href=\"(.+?)\"");
		Matcher urlMatcher = urlPattern.matcher(content);
		boolean isFind = urlMatcher.find();
		while (isFind) {
			// ��ӳɹ�ƥ��Ľ��
			bookLinks.add(urlMatcher.group(1));
			// ����������һ��ƥ�����
			isFind = urlMatcher.find();
		}
		return bookLinks;
	}

	// ���б�����ȡbook��url
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

	// ��ȡ��ǰҳ�������������
	public DoubanBookInfo getBookInfo(String url) {
		String content = SendGet(url);
		// ����ƥ������
		Pattern titlePattern = Pattern.compile("nbg.+?alt=\"(.+?)\"");
		Matcher titleMatcher = titlePattern.matcher(content);
		// ����ƥ������
		Pattern authorPattern = Pattern.compile("href=\"/search.+?>(.+?)<");
		Matcher authorMatcher = authorPattern.matcher(content);
		// ����ƥ�������
		Pattern publisherPattern = Pattern.compile("������:</span>(.+?)<");
		Matcher publisherMatcher = publisherPattern.matcher(content);
		// ����ƥ�丱����
		Pattern subTitlePattern = Pattern.compile("������:</span>(.+?)<");
		Matcher subTitleMatcher = subTitlePattern.matcher(content);
		// ����ƥ��ԭ����
		Pattern originalNamePattern = Pattern.compile("ԭ����:</span>(.+?)<");
		Matcher originalNameMatcher = originalNamePattern.matcher(content);
		// ����ƥ������
		Pattern translaterPattern = Pattern.compile("����.+?href.+?>(.+?)<");
		Matcher translaterMatcher = translaterPattern.matcher(content);
		// ����ƥ�������
		Pattern publishYearPattern = Pattern.compile("������:</span>(.+?)<");
		Matcher publishYearMatcher = publishYearPattern.matcher(content);
		// ����ƥ��ҳ��
		Pattern pagePattern = Pattern.compile("ҳ��:</span>(.+?)<");
		Matcher pageMatcher = pagePattern.matcher(content);
		// ����ƥ�䶨��
		Pattern pricePattern = Pattern.compile("����:</span>(.+?)<");
		Matcher priceMatcher = pricePattern.matcher(content);
		// ����ƥ��װ֡
		Pattern bookBindingPattern = Pattern.compile("װ֡:</span>(.+?)<");
		Matcher bookBindingNameMatcher = bookBindingPattern.matcher(content);
		// ����ƥ�����
		Pattern seriesPattern = Pattern.compile("����.+?href.+?>(.+?)<");
		Matcher seriesMatcher = seriesPattern.matcher(content);
		// ����ƥ��ISBN
		Pattern ISBNPattern = Pattern.compile("ISBN:</span>(.+?)<");
		Matcher ISBNMatcher = ISBNPattern.matcher(content);
		// ����ƥ�����
		Pattern ratePattern = Pattern.compile("average\">(.+?)<");
		Matcher rateMatcher = ratePattern.matcher(content);
		// ����ƥ��ISBN
		Pattern votesPattern = Pattern.compile("votes\">(.+?)<");
		Matcher votesMatcher = votesPattern.matcher(content);

		// ����һ���������������洢ץȡ������Ϣ
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
