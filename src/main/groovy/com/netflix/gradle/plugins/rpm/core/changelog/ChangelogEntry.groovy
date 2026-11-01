/*
 * Ported to Groovy from Redline (https://github.com/craigwblake/redline).
 * Copyright (c) 2007-2015 FreeCompany. Licensed under the MIT License,
 * see META-INF/licenses/redline-LICENSE.txt (src/main/resources).
 */
package com.netflix.gradle.plugins.rpm.core.changelog

import groovy.transform.CompileStatic

/**
 * A single entry of an RPM changelog, for example:
 * <pre>
 * * Tue Feb 24 2015 George Washington
 *   - Add the foo feature
 *     Add the bar feature
 * </pre>
 */
@CompileStatic
class ChangelogEntry {

    private Date changeLogTime
    private String userMakingChange
    /**
     * The text of the entry: all lines after the date line up to (not including) the next line
     * beginning with an asterisk, which starts a new entry.
     */
    private String description

    ChangelogEntry() {
    }

    ChangelogEntry(Date changeLogTime, String userMakingChange, String description) {
        this.changeLogTime = changeLogTime
        this.userMakingChange = userMakingChange
        this.description = description
    }

    /**
     * @return true if the entry has a date, a user and a description
     */
    boolean isComplete() {
        return changeLogTime != null && userMakingChange != null && description != null
    }

    Date getChangeLogTime() {
        return changeLogTime
    }

    void setChangeLogTime(Date changeLogTime) {
        this.changeLogTime = changeLogTime
    }

    String getUserMakingChange() {
        return userMakingChange
    }

    void setUserMakingChange(String userMakingChange) {
        this.userMakingChange = userMakingChange
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }
}
