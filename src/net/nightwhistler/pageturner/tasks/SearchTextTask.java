/*
 * Copyright (C) 2013 Alex Kuiper
 *
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */

package net.nightwhistler.pageturner.tasks;

import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.SpanStack;
import net.nightwhistler.htmlspanner.TagNodeHandler;
import net.nightwhistler.htmlspanner.handlers.TableHandler;
import net.nightwhistler.pageturner.epub.PageTurnerSpine;
import nl.siegmann.epublib.domain.Book;
import org.htmlcleaner.TagNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTextTask extends AsyncTask<String, SearchTextTask.SearchResult, List<SearchTextTask.SearchResult>> {
	
	private Book book;
	
	private HtmlSpanner spanner;
	
	public SearchTextTask(Book book) {
		this.book = book;
		
		this.spanner = new HtmlSpanner();
		
		DummyHandler dummy = new DummyHandler();
		
        spanner.registerHandler("img", dummy );
        spanner.registerHandler("image", dummy );
        
        spanner.registerHandler("table", new TableHandler() );
	}	
	

	@Override
	protected List<SearchTextTask.SearchResult> doInBackground(String... params) {
		
		String searchTerm = params[0];
		Pattern pattern = Pattern.compile(Pattern.quote((searchTerm)),Pattern.CASE_INSENSITIVE);
		
		List<SearchTextTask.SearchResult> result = new ArrayList<SearchTextTask.SearchResult>();
		
		try {

			PageTurnerSpine spine = new PageTurnerSpine(book);
			
			
			
			for ( int index=0; index < spine.size(); index++ ) {
				
				spine.navigateByIndex(index);
				
				int progress = spine.getProgressPercentage(index, 0);
								
				publishProgress( new SearchResult(null, index, 0, 0, progress) );
				
				Spanned spanned = spanner.fromHtml(spine.getCurrentResource().getReader());				
				Matcher matcher = pattern.matcher(spanned);
				
				while ( matcher.find() ) {
					int from = Math.max(0, matcher.start() - 20 );
					int to = Math.min(spanned.length() -1, matcher.end() + 20 );
					
					if ( isCancelled() ) {
						return null;
					}
					
					String text = "…" + spanned.subSequence(from, to).toString().trim() + "…";
					SearchResult res = new SearchResult(text, index, matcher.start(), matcher.end(),
							spine.getProgressPercentage(index, matcher.start()));
					
					this.publishProgress( res );
					result.add(res);
				}
				
			}
		} catch (IOException io) {
			return null;
		}

		return result;		
	}
	
	public static class SearchResult {
		
		private String display;
		private int index;
		private int start;
		private int end;
		
		private int percentage;
		
		public SearchResult(String display, int index, int offset, int end, int percentage ) {
			this.display = display;
			this.index = index;
			this.start = offset;
			this.end = end;
			this.percentage = percentage;
		}
		
		public String getDisplay() {
			return display;
		}
		
		public int getIndex() {
			return index;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}
		
		public int getPercentage() {
			return percentage;
		}
		
	}
	
	private static class DummyHandler extends TagNodeHandler {
		@Override
		public void handleTagNode(TagNode node, SpannableStringBuilder builder,
				int start, int end, SpanStack stack) {

			 builder.append("\uFFFC");
		}
	}
}
