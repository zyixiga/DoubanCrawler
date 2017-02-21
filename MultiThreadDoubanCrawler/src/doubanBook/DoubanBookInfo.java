package doubanBook;

public class DoubanBookInfo {

	private String title;
	private String author;
	private String publisher;
	private String subTitle;
	private String originalName;
	private String translater;
	private String publishYear;
	private String pages;
	private String price;
	private String bookBinding;
	private String series;
	private String ISBN;
	private double rate;
	private int votes;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getTranslater() {
		return translater;
	}

	public void setTranslater(String translater) {
		this.translater = translater;
	}

	public String getPublishYear() {
		return publishYear;
	}

	public void setPublishYear(String publishYear) {
		this.publishYear = publishYear;
	}

	public String getPages() {
		return pages;
	}

	public void setPages(String pages) {
		this.pages = pages;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getBookBinding() {
		return bookBinding;
	}

	public void setBookBinding(String bookBinding) {
		this.bookBinding = bookBinding;
	}

	public String getSeries() {
		return series;
	}

	public void setSeries(String series) {
		this.series = series;
	}

	public String getISBN() {
		return ISBN;
	}

	public void setISBN(String iSBN) {
		ISBN = iSBN;
	}

	public double getRate() {
		return rate;
	}

	public void setRate(double rate) {
		this.rate = rate;
	}

	public int getVotes() {
		return votes;
	}

	public void setVotes(int votes) {
		this.votes = votes;
	}

	@Override
	public String toString() {
		return "\"" + title + "\",\"" + author + "\",\"" + publisher + "\",\"" + subTitle + "\",\"" + originalName
				+ "\",\"" + pages + "\",\"" + ISBN + "\",\"" + rate + "\",\"" + votes + "\"";
	}

}
