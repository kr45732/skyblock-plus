/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.utils;

import java.awt.image.*;
import java.io.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;

public class GifWriter implements Closeable {

	private final ImageWriter gifWriter;
	private final ImageWriteParam imageWriteParam;
	private final IIOMetadata imageMetaData;

	public GifWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMS, boolean loopContinuously, boolean transparentColorFlag) throws IOException {
		gifWriter = getWriter();
		imageWriteParam = gifWriter.getDefaultWriteParam();
		ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
		imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);
		String metaFormatName = imageMetaData.getNativeMetadataFormatName();
		IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);
		IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
		graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
		graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
		graphicsControlExtensionNode.setAttribute("transparentColorFlag", transparentColorFlag ? "TRUE" : "FALSE");
		graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
		graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");
		IIOMetadataNode appExtensionsNode = getNode(root, "ApplicationExtensions");
		IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
		child.setAttribute("applicationID", "NETSCAPE");
		child.setAttribute("authenticationCode", "2.0");
		int loop = loopContinuously ? 0 : 1;
		child.setUserObject(new byte[] { 0x1, (byte) (loop & 0xFF), (byte) 0 });
		appExtensionsNode.appendChild(child);
		imageMetaData.setFromTree(metaFormatName, root);
		gifWriter.setOutput(outputStream);
		gifWriter.prepareWriteSequence(null);
	}

	public GifWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMS, boolean loopContinuously) throws IOException {
		this(outputStream, imageType, timeBetweenFramesMS, loopContinuously, false);
	}

	public void writeToSequence(RenderedImage img) throws IOException {
		gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
	}

	public void close() throws IOException {
		gifWriter.endWriteSequence();
	}

	private ImageWriter getWriter() throws IIOException {
		Iterator<ImageWriter> iterator = ImageIO.getImageWritersBySuffix("gif");
		if (!iterator.hasNext()) {
			throw new IIOException("No GIF Image Writers Exist");
		} else {
			return iterator.next();
		}
	}

	private IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
		int nNodes = rootNode.getLength();
		for (int i = 0; i < nNodes; i++) {
			if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
				return ((IIOMetadataNode) rootNode.item(i));
			}
		}
		IIOMetadataNode node = new IIOMetadataNode(nodeName);
		rootNode.appendChild(node);
		return node;
	}
}
