package com.testfabrik.webmate.javasdk.testmgmt;

import org.joda.time.DateTime;

import java.util.Objects;

/**
 * Information about a Test.
 */
public class TestInfo {
    private TestId id;
    private String name;
    private DateTime creationTime;
    private String description;
    private int version;

    private TestInfo() {}

    public TestInfo(TestId id, String name, DateTime creationTime, String description, int version) {
        this.id = id;
        this.name = name;
        this.creationTime = creationTime;
        this.description = description;
        this.version = version;
    }

    /**
     * Id of Test.
     */
    public TestId getId() {
        return id;
    }

    /**
     * Name of Test.
     */
    public String getName() {
        return name;
    }

    /**
     * Time when the Test was created.
     */
    public DateTime getCreationTime() {
        return creationTime;
    }

    /**
     * Human readable description of Test.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Version of Test document.
     */
    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestInfo testInfo = (TestInfo) o;
        return version == testInfo.version &&
                id.equals(testInfo.id) &&
                name.equals(testInfo.name) &&
                creationTime.equals(testInfo.creationTime) &&
                description.equals(testInfo.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, creationTime, description, version);
    }

    @Override
    public String toString() {
        return "TestInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", creationTime=" + creationTime +
                ", description='" + description + '\'' +
                ", version=" + version +
                '}';
    }
}