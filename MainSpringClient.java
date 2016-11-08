package com.capitalone.dashboard.collector;

import java.util.List;

import com.capitalone.dashboard.model.MainSpring;
import com.capitalone.dashboard.model.MainSpringProject;

public interface MainSpringClient { 

    List<MainSpringProject> getProjects(String instanceUrl );
//Test comment for Hygieia - dashboard
 List<MainSpring> currentMainSpringAnalysis(MainSpringProject project);

}
