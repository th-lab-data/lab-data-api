package de.steinhae.lab.lambda.objects;

import java.util.Objects;

public class Page {
    private int current;
    private int returnedElements;
    private int totalElements;
    private int minPage;
    private int maxPage;

    public Page(int current, int returnedElements, int totalElements, int minPage, int maxPage) {
        this.current = current;
        this.returnedElements = returnedElements;
        this.totalElements = totalElements;
        this.maxPage = maxPage;
        this.minPage = minPage;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getReturnedElements() {
        return returnedElements;
    }

    public void setReturnedElements(int returnedElements) {
        this.returnedElements = returnedElements;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getMinPage() {
        return minPage;
    }

    public void setMinPage(int minPage) {
        this.minPage = minPage;
    }

    public int getMaxPage() {
        return maxPage;
    }

    public void setMaxPage(int maxPage) {
        this.maxPage = maxPage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return current == page.current &&
            returnedElements == page.returnedElements &&
            totalElements == page.totalElements &&
            minPage == page.minPage &&
            maxPage == page.maxPage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(current, returnedElements, totalElements, minPage, maxPage);
    }

    @Override
    public String toString() {
        return "Page{" +
            "current=" + current +
            ", returnedElements=" + returnedElements +
            ", totalElements=" + totalElements +
            ", minPage=" + minPage +
            ", maxPage=" + maxPage +
            '}';
    }
}
