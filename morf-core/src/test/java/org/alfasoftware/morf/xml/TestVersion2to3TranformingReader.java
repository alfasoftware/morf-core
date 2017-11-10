package org.alfasoftware.morf.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;

public class TestVersion2to3TranformingReader {



  private BufferedReader makeOutputReader(LinkedList<String> responses) throws IOException {
    BufferedReader sourceReader = Mockito.mock(BufferedReader.class);

    final java.util.LinkedList<String> markedResponses = new LinkedList<>();

    Mockito.when(sourceReader.read(Mockito.any(char[].class), Mockito.anyInt(), Mockito.anyInt())).thenAnswer((invocation) -> {
      char[] inputChars = (char[]) invocation.getArguments()[0];
      int inputOffset = (int) invocation.getArguments()[1];
      int inputLength = (int) invocation.getArguments()[2];

      if (responses.isEmpty()) {
        return -1;
      }

      String response = responses.remove();
      if (response.length() > inputLength) {
        responses.addFirst(response.substring(inputLength));
      }

      int toCopy = Math.min(response.length(), inputLength);
      System.arraycopy(response.toCharArray(), 0, inputChars, inputOffset, toCopy);
      return toCopy;
    });

    Mockito.doAnswer((invocation) -> {
      markedResponses.addAll(responses);
      return null;
    }).when(sourceReader).mark(Mockito.anyInt());

    Mockito.doAnswer((invocation) -> {
      responses.clear();
      responses.addAll(markedResponses);
      return null;
    }).when(sourceReader).reset();


    Mockito.when(sourceReader.markSupported()).thenReturn(true);

    return new BufferedReader(new Version2to3TranformingReader(sourceReader));
  }

  /**
   * @param expected The expected ouput
   * @param input The list of fragments to supply from the mock BufferedReader
   */
  private void runTest(String expected, List<String> input) throws IOException {
    Assert.assertEquals(
      expected,
      makeOutputReader(new LinkedList<>(input)).lines().collect(Collectors.joining())
    );
  }

  @Test
  public void testWithoutTransform() throws IOException {
    runTest("one, two.", ImmutableList.of("one, ", "two."));
    runTest("An &amp; should not be transformed", ImmutableList.of("An &amp; should not be transformed"));
    runTest("Not quite a null &#0", ImmutableList.of("Not quite a null &#0"));
    runTest("Not quite a null &0#; this", ImmutableList.of("Not quite a null &0#; this"));
    runTest("Not quite a null &#0 this", ImmutableList.of("Not quite a null &","#0 this"));
    runTest("Not quite a null &#0 this", ImmutableList.of("Not quite a null &#","0 this"));
    runTest("Not quite a null &#0 this", ImmutableList.of("Not quite a null &#0"," this"));
  }


  @Test
  public void testTranform() throws IOException {
    runTest("1 An \\0 should be transformed", ImmutableList.of("1 An &#0; should be transformed"));
    runTest("2 An \\0 should be transformed", ImmutableList.of("2 An &#0;", " should be transformed"));
    runTest("3 An \\0 should be transformed", ImmutableList.of("3 An ", "&#0; should be transformed"));
    runTest("4 An \\0 should be transformed", ImmutableList.of("4 An &", "#0;"," should be transformed"));
    runTest("5 An \\0 should be transformed", ImmutableList.of("5 An &", "#", "0", ";", " should be transformed"));
    runTest("6 An \\0 should be transformed", ImmutableList.of("6 An &#", "0", ";", " should be transformed"));
    runTest("7 An \\0 should be transformed", ImmutableList.of("7 An &#", "0;", " should be transformed"));
    runTest("8 Multiple \\0 transforms \\0 needed", ImmutableList.of("8 Multiple &#0; transforms &#0; needed"));
  }

}