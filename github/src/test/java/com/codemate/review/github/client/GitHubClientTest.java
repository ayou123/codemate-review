package com.codemate.review.github.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

class GitHubClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(options().dynamicPort()).build();

    private String fixture(String name) throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/" + name));
    }

    @Test
    void fetchesPRMeta() throws Exception {
        wm.stubFor(get(urlEqualTo("/repos/o/r/pulls/1"))
            .willReturn(okJson(fixture("pr.json"))));
        var c = new GitHubClient(wm.baseUrl(), "tok");
        PRInfo pr = c.fetchPR("o","r",1);
        assertThat(pr.title()).isEqualTo("Add foo");
        assertThat(pr.headSha()).hasSize(40);
        assertThat(pr.baseSha()).hasSize(40);
    }

    @Test
    void fetchesDiff() throws Exception {
        wm.stubFor(get(urlEqualTo("/repos/o/r/pulls/1"))
            .withHeader("Accept", equalTo("application/vnd.github.v3.diff"))
            .willReturn(aResponse().withStatus(200).withBody(fixture("pr.diff"))));
        var diff = new GitHubClient(wm.baseUrl(), "tok").fetchDiff("o","r",1);
        assertThat(diff).startsWith("diff --git");
    }

    @Test
    void fetchesFileContents() throws Exception {
        wm.stubFor(get(urlEqualTo("/repos/o/r/contents/Foo.java?ref=abc123"))
            .willReturn(okJson(fixture("file-base64.json"))));
        Optional<String> body = new GitHubClient(wm.baseUrl(), "tok").fetchFile("o","r","abc123","Foo.java");
        assertThat(body).isPresent();
        assertThat(body.get()).contains("class");
    }

    @Test
    void fetchFileReturnsEmptyOn404() {
        wm.stubFor(get(urlMatching("/repos/o/r/contents/.codemate.yml.*"))
            .willReturn(aResponse().withStatus(404)));
        assertThat(new GitHubClient(wm.baseUrl(), "tok").fetchFile("o","r","sha",".codemate.yml")).isEmpty();
    }

    @Test
    void listsJavaFilesRecursively() {
        wm.stubFor(get(urlMatching("/repos/o/r/git/trees/sha.*"))
            .willReturn(okJson("""
                {"tree":[
                  {"path":"src/main/java/A.java","type":"blob"},
                  {"path":"src/main/java/B.java","type":"blob"},
                  {"path":"README.md","type":"blob"},
                  {"path":"src","type":"tree"}
                ]}""")));
        var c = new GitHubClient(wm.baseUrl(), "tok");
        List<String> files = c.listRootJavaFiles("o","r","sha");
        assertThat(files).containsExactly("src/main/java/A.java", "src/main/java/B.java");
    }

    @Test
    void sendsPATAsBearerToken() throws Exception {
        wm.stubFor(get(urlEqualTo("/repos/o/r/pulls/1"))
            .withHeader("Authorization", equalTo("Bearer pat-token"))
            .willReturn(okJson(fixture("pr.json"))));
        new GitHubClient(wm.baseUrl(), "pat-token").fetchPR("o","r",1);
        wm.verify(getRequestedFor(urlEqualTo("/repos/o/r/pulls/1"))
            .withHeader("Authorization", equalTo("Bearer pat-token")));
    }
}
