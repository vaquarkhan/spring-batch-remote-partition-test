/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.partition.support;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Creates a set of partitions for a flat text file.
 * <p/>
 * Assumes that each record is stored on one and only one line.
 * Reads the file's byte stream detecting line ends and creates partitions
 * splitted at the new line border. Populates the {@link ExecutionContext} with
 * the byte offset for each partition thread and number of items/lines to be read from that position.
 * <p/>
 * Can be used to read the file concurrently. Each partition thread should use the byte offset specified by the
 * <tt>startAt</tt> 
 * offset to set cursor at the starting position and a number of items (lines) to read as defined 
 * by the <tt>itemsCount</tt> property.
 *
 * @author Sergey Shcherbakov
 * @author Stephane Nicoll
 */
public class FlatFilePartitioner implements Partitioner {

    /**
     * The {@link ExecutionContext} key name for the number of bytes the partition should skip on startup.
     */
    public static final String DEFAULT_START_AT_KEY = "startAt";

    /**
     * The {@link ExecutionContext} key name for number of items/lines to read in the partition.
     */
    public static final String DEFAULT_ITEMS_COUNT_KEY = "itemsCount";
    
    /**
     * The {@link ExecutionContext} key name for number of previous partition's items/lines readed
     */
    public static final String DEFAULT_PREVIOUS_ITEMS_COUNT_KEY = "previousItemsCount";
    
    /**
     * The {@link ExecutionContext} key name for the file resource which has been used for partitioning.
     */
	public static final String DEFAULT_RESOURCE_KEY = "resource";

    /**
     * The common partition prefix name to use.
     */
    public static final String DEFAULT_PARTITION_PREFIX = "partition-";
    
    /**
     * Default buffer size to use when reading the file through during partitioning
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;
    
    /**
     * Default character that breaks input byte stream into lines
     */
    public static final char DEFAULT_LINE_SEPARATOR_CHAR = '\n';

    public static final int DEFAULT_LINES_TO_SKIP = 0;

    //private final Logger logger = LoggerFactory.getLogger(FlatFilePartitioner.class);
    private static Log logger = LogFactory.getLog(FlatFilePartitioner.class);

    private Resource resource;

    private String startAtKeyName = DEFAULT_START_AT_KEY;
    private String itemsCountKeyName = DEFAULT_ITEMS_COUNT_KEY;
    private String previousItemsCountKeyName = DEFAULT_PREVIOUS_ITEMS_COUNT_KEY;
    private String resourceKeyName = DEFAULT_RESOURCE_KEY;
    private String partitionPrefix = DEFAULT_PARTITION_PREFIX;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private char lineSeparatorCharacter = DEFAULT_LINE_SEPARATOR_CHAR;
	private int linesToSkip = DEFAULT_LINES_TO_SKIP;
    
	/**
	 * Public setter for the number of lines to skip at the start of a file. Can be used if the file contains a header
	 * without useful (column name) information, and without a comment delimiter at the beginning of the lines.
	 * 
	 * @param linesToSkip the number of lines to skip
	 */
	public void setLinesToSkip(int linesToSkip) {
		this.linesToSkip = linesToSkip;
	}
	/**
	 * The name of the key for the byte offset in each {@link ExecutionContext}.
	 * Defaults to "startAt".
	 * @param keyName the value of the key
	 */
	public void setStartAtKeyName(String keyName) {
		this.startAtKeyName = keyName;
	}

	/**
	 * The name of the key for the byte offset in each {@link ExecutionContext}.
	 * Defaults to "itemsCount".
	 * @param keyName the value of the key
	 */
	public void setItemsCountKeyName(String keyName) {
		this.itemsCountKeyName = keyName;
	}

	/**
	 * The name of the key for the line offset in each {@link ExecutionContext}.
	 * Defaults to "previousItemsCount".
	 * @param keyName the value of the key
	 */
	public void setPreviousItemsCountKeyName(String keyName) {
		this.previousItemsCountKeyName = keyName;
	}

	/**
	 * The name of the key for the file name in each {@link ExecutionContext}.
	 * Defaults to "resource".
	 * @param keyName the value of the key
	 */
	public void setResourceKeyName(String keyName) {
		this.resourceKeyName = keyName;
	}

	/**
	 * The prefix used to prepend each generated partition name 
	 * @param prefix
	 */
	public void setPartitionPrefix(String prefix) {
		this.partitionPrefix = prefix;
	}
	
	/**
     * The buffer size to use when reading the file through during partitioning
	 * @param bufferSize
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
     * Sets the character that breaks input byte stream into lines
	 */
	public void setLineSeparatorCharacter(char lineSeparatorChar) {
		this.lineSeparatorCharacter = lineSeparatorChar;
	}
	
	public static class LinesCount {
		private long bytesToSkip;
		private long linesCount;
		public LinesCount(long bytesToSkip, long linesCount) {
			this.bytesToSkip = bytesToSkip;
			this.linesCount = linesCount;
		}
		public long getBytesToSkip() {
			return bytesToSkip;
		}
		public long getLinesCount() {
			return linesCount;
		}
	}
	
    /**
     * Creates a set of {@link ExecutionContext} according to the provided
     * <tt>gridSize</tt> if there are enough elements.
     * <p/>
     * First computes the total number of items to process for the resource
     * and then split equality these in each partition. The returned context
     * hold the {@link #DEFAULT_START_AT_KEY} and {@link #DEFAULT_ITEMS_COUNT_KEY} properties
     * defining the number of elements to skip and the number of elements to
     * read respectively.
     *
     * @param gridSize the requested size of the grid
     * @return the execution contexts
     * @see #countItems(org.springframework.core.io.Resource)
     */
    public Map<String, ExecutionContext> partition(int gridSize) {
		Assert.isTrue(gridSize > 0, "Grid size must be greater than 0");

        checkResource(this.resource);
        if (logger.isDebugEnabled()) {
            logger.debug("Splitting [" + resource.getDescription() + "]");
        }
        try {
	        final Map<String, ExecutionContext> result = new LinkedHashMap<String, ExecutionContext>();
	        
	        final long sizeInBytes = resource.contentLength();
	        if (sizeInBytes == 0) {
	            logger.info("Empty input file [" + resource.getDescription() + "] no partition will be created.");
	            return result;
	        }

	        PartitionBorderCursor partitionCursor = new PartitionBorderCursor(gridSize, sizeInBytes); 
		        
	        // Check the case that the set is to small for the number of request partition(s)
	        if (partitionCursor.getBytesPerPartition() == 0) {
	        	LinesCount linesCount = countItems(resource);
	            logger.info("Not enough data (" + linesCount.getLinesCount() + ") for the requested gridSize [" + gridSize + "]");
	            partitionCursor.createPartition( linesCount, result );
	            return result;
	        }

	        if (logger.isDebugEnabled()) {
	            logger.debug("Has to split [" + sizeInBytes + "] byte(s) in [" + gridSize + "] " +
	                    "grid(s) (" + partitionCursor.getBytesPerPartition() + " each)");
	        }

            final InputStream in = resource.getInputStream();
        	try {
	            final InputStream is = new BufferedInputStream(in);
				byte[] c = new byte[bufferSize];
				ByteStreamCursor byteCursor = new ByteStreamCursor(); 
	            int readChars;
	            
	            while ((readChars = is.read(c)) != -1) {
	                for (int i = 0; i < readChars; ++i) {
	                	if( byteCursor.lastSeenCharIsNewline( c[i] ) ) {
		                	if( byteCursor.getCurrentByteInd() > partitionCursor.getPartitionBorder() ) {
		                		partitionCursor.createPartition( byteCursor.getLinesCount(), result );
		    	            	byteCursor.startNewPartition();
		                	}
	                    }
	                }
	            }
	            if ( byteCursor.lastLineUnterminated() ) {
	            	byteCursor.startNewLine();
	            }
	            if( byteCursor.outstandingData() ) {
	            	partitionCursor.createPartition( byteCursor.getLinesCount(), result );
	            }
		        return result;
        	}
        	finally {
                in.close();
        	}
        }
        catch (IOException e) {
            throw new IllegalStateException("Unexpected IO exception while partitioning [" + resource.getDescription() + "]", e);
        }
    }
    
    /**
     * This is a helper class to simplify the byte stream iterating code.
     * Tracks current location in the byte stream, number of lines counted from the
     * last partition start and from the input stream beginning.
     * Increments indexes on a new character read. 
     * Detects the new line character and updates counters.
     */
    private class ByteStreamCursor {
        private long totalLineCount = 0;
        private long lineCount = 0;
        private long skipLineCount = linesToSkip;
        private long skipBytesCount = 0;
        private byte lastSeenChar = 0;
        private long currentByteInd = 0L;
        private long startAt = 0;
        
		public boolean lastSeenCharIsNewline(byte lastSeenChar) {
			this.lastSeenChar = lastSeenChar;
			this.currentByteInd++;
			if(skipLineCount > 0) {
				skipBytesCount++;
			}
            // New line is \n on Unix and \r\n on Windows                
            if (lastSeenChar == lineSeparatorCharacter) {
            	startNewLine();
                return true;
            }
            return false;
		}
		
		public void startNewLine() {
			if(skipLineCount > 0) {
				skipLineCount--;
			}
			else {
				lineCount++;
			}
            totalLineCount++;
		}

		public void startNewPartition() {
            startAt = currentByteInd;
            lineCount = 0;
		}

		public LinesCount getLinesCount() {
			return new LinesCount(startAt > skipBytesCount ? startAt : skipBytesCount, lineCount);
		}

		public long getCurrentByteInd() {
			return currentByteInd;
		}
		
		public boolean lastLineUnterminated() {
			return (totalLineCount > 0 && lastSeenChar != lineSeparatorCharacter) || 						// <-- last line is not empty but is not terminated by '\n'
	            (totalLineCount == 0 && lastSeenChar != lineSeparatorCharacter && currentByteInd > 0);	// <-- the first line is the last line and it's not terminated by '\n'
		}
		
		public boolean outstandingData() {
			return currentByteInd > 0 && startAt != currentByteInd;
		}
    }

    /**
     * This is a helper class to simplify the byte stream iterating code.
     * Tracks the location of approximate byte offsets that split the input file into
     * approximately (+/-1) equal byte partitions.
     * When the main iteration passes this border the next partition will be created as soon
     * as the next new line character or end of stream is detected.
     */
    private class PartitionBorderCursor {
    	private int gridSize;
        private final long bytesPerPartition;
        private final long bytesRemainder;
        private long remainderCounter;
        private long partitionBorder;
        private int partitionIndex;
        private long previousItemsCount;

    	PartitionBorderCursor(int gridSize, long sizeInBytes) {
    		this.gridSize = gridSize;
            this.bytesPerPartition = sizeInBytes / gridSize;
            this.bytesRemainder = sizeInBytes % gridSize;
            this.remainderCounter = this.bytesRemainder;
            this.partitionBorder = 0;
            this.partitionIndex = 0;
            this.previousItemsCount = 0;
			toNextPartitionBorder();
    	}

		public long getBytesPerPartition() {
			return bytesPerPartition;
		}
		
		public long getPartitionBorder() {
			return this.partitionBorder;
		}
		
		private void toNextPartitionBorder() {
			this.partitionBorder += bytesPerPartition + (remainderCounter-- > 0 ? 1 : 0);
		}
		
		public void createPartition(LinesCount linesCount, final Map<String, ExecutionContext> result) {

			final String partitionName = getPartitionName(gridSize, partitionIndex++);
			result.put(partitionName, createExecutionContext(partitionName, linesCount.getBytesToSkip(), linesCount.getLinesCount(), previousItemsCount));
			previousItemsCount += linesCount.getLinesCount();
			toNextPartitionBorder();
		}
		
		private String getPartitionName(int gridSize, int partitionIndex) {
			final String partitionNumberFormat = "%0" + String.valueOf(gridSize).length() + "d";
			return partitionPrefix + String.format(partitionNumberFormat, partitionIndex);
		}
    }
    
    /**
     * Creates a standard {@link ExecutionContext} with the specified parameters.
     * @param partitionName the name of the partition
     * @param startAt the number of bytes for a partition thread to skip before starting reading
     * @param itemsCount the number of items to read
     * @return the execution context (output)
     */
    protected ExecutionContext createExecutionContext(String partitionName, long startAt, long itemsCount, long previousItemsCount) {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong(startAtKeyName, startAt);
        executionContext.putLong(itemsCountKeyName, itemsCount);
        executionContext.putLong(previousItemsCountKeyName, previousItemsCount);
		try {
			executionContext.putString(resourceKeyName, "file:" + resource.getFile().getPath());
		} catch (IOException e) {
			throw new IllegalArgumentException("File could not be located for: "+resource, e);
		}
		if (logger.isDebugEnabled()) {
            logger.debug("Added partition [" + partitionName + "] with [" + executionContext + "]");
        }
        return executionContext;
    }

    /**
     * Returns the number of elements in the specified {@link Resource}.
     *
     * @param resource the resource
     * @return the number of items contained in the resource
     */
    protected LinesCount countItems(Resource resource) {
        try {
            final InputStream in = resource.getInputStream();
            try {
                return countLinesAfterSkip(in);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IO exception while counting items for ["
                    + resource.getDescription() + "]", e);
        }
    }
    
    private LinesCount countLinesAfterSkip(InputStream in) throws IOException {
        final InputStream is = new BufferedInputStream(in);
        byte[] c = new byte[bufferSize];
        long count = 0;
        int readChars;
        byte lastChar = 0;
        boolean contentExists = false;
        long lineSkipCount = linesToSkip;
        long bytesToSkip = 0;
        while ((readChars = is.read(c)) != -1) {
            for (int i = 0; i < readChars; ++i) {
            	contentExists = true;
            	lastChar = c[i];
            	if( lineSkipCount > 0) {
            		bytesToSkip++;
            	}
                // We're dealing with the char here, it's \n on Unix and \r\n on Windows                
                if (c[i] == DEFAULT_LINE_SEPARATOR_CHAR) {
                	if( lineSkipCount > 0 ) {
                		lineSkipCount--;
                	}
                	else {
                		count++;
                	}
                }
            }
        }
        // Last line
        if ( (count > 0 && lastChar != DEFAULT_LINE_SEPARATOR_CHAR) || 						// <-- last line is not empty but is not terminated by '\n'
        	(count == 0 && lastChar != DEFAULT_LINE_SEPARATOR_CHAR && contentExists) ) {		// <-- the first line is the last line and it's not terminated by '\n'
        	if( lineSkipCount > 0 ) {
        		lineSkipCount--;
        	}
        	else {
        		count++;
        	}
        }
        return new LinesCount(bytesToSkip, count);
    }

    /**
     * Returns the number of lines found in the specified stream.
     * <p/>
     * The caller is responsible to close the stream.
     *
     * Up to 5 times faster than using BufferedReader and up to 2 times faster 
     * than LineNumberReader.
     * 
     * @param in the input stream to use
     * @return the number of lines found in the stream
     * @throws IOException if an error occurred
     */    
    public static long countLines(InputStream in) throws IOException {
        final InputStream is = new BufferedInputStream(in);
        byte[] c = new byte[DEFAULT_BUFFER_SIZE];
        long count = 0;
        int readChars;
        byte lastChar = 0;
        boolean contentExists = false;
        while ((readChars = is.read(c)) != -1) {
            for (int i = 0; i < readChars; ++i) {
            	contentExists = true;
            	lastChar = c[i];
                // We're dealing with the char here, it's \n on Unix and \r\n on Windows                
                if (c[i] == DEFAULT_LINE_SEPARATOR_CHAR)
                    ++count;
            }
        }
        // Last line
        if ( (count > 0 && lastChar != DEFAULT_LINE_SEPARATOR_CHAR) || 						// <-- last line is not empty but is not terminated by '\n'
        	(count == 0 && lastChar != DEFAULT_LINE_SEPARATOR_CHAR && contentExists) ) {		// <-- the first line is the last line and it's not terminated by '\n'
            count++;
        }
        return count;
    }

    /**
     * Checks whether the specified {@link Resource} is valid.
     *
     * @param resource the resource to check
     * @throws IllegalStateException if the resource is invalid
     */
    protected void checkResource(Resource resource) {
    	Assert.notNull(resource, "Resource is not set");
        if (!resource.exists()) {
            throw new IllegalStateException("Input resource must exist: " + resource);
        }
        if (!resource.isReadable()) {
            throw new IllegalStateException("Input resource must be readable: " + resource);
        }
    }

    /**
     * Sets the input {@link Resource} to use.
     *
     * @param resource the resource to partition
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
