package org.burningokr.service.okrUnitUtil;

import java.util.Collection;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.burningokr.model.cycles.Cycle;
import org.burningokr.model.okr.Objective;
import org.burningokr.model.okrUnits.*;
import org.burningokr.model.settings.UserSettings;
import org.burningokr.repositories.okr.ObjectiveRepository;
import org.burningokr.repositories.okrUnit.CompanyRepository;
import org.burningokr.repositories.okrUnit.UnitRepository;
import org.burningokr.repositories.settings.UserSettingsRepository;
import org.burningokr.service.okrUnit.departmentservices.BranchHelper;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Service
@RequiredArgsConstructor
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CyclePreparationCloningService {

  private HashMap<Objective, Objective> clonedObjectives = new HashMap<>();

  private final CompanyRepository companyRepository;
  private final UnitRepository<OkrChildUnit> subUnitRepository;
  private final ObjectiveRepository objectiveRepository;
  private final UserSettingsRepository userSettingsRepository;

  public void cloneCompanyListIntoCycleForPreparation(
      Collection<OkrCompany> companiesToClone, Cycle cycleToCloneInto) {
    companiesToClone.forEach(
        company -> cloneCompanyIntoCycleForPreparation(company, cycleToCloneInto));
  }

  /**
   * Clones the OkrCompany and adds it to the Cycle.
   *
   * @param okrCompanyToClone a {@link OkrCompany} object
   * @param cycleToCloneInto a {@link Cycle} object
   */
  public void cloneCompanyIntoCycleForPreparation(
      OkrCompany okrCompanyToClone, Cycle cycleToCloneInto) {
    OkrCompany okrCompanyCopy = okrCompanyToClone.getCopyWithoutRelations();
    cycleToCloneInto.getCompanies().add(okrCompanyCopy);
    okrCompanyCopy.setCycle(cycleToCloneInto);
    companyRepository.save(okrCompanyCopy);
    cloneObjectiveListIntoOkrUnitForPreparation(okrCompanyToClone.getObjectives(), okrCompanyCopy);
    cloneChildUnitListIntoParentUnitForPreparation(
        okrCompanyToClone.getOkrChildUnits(), okrCompanyCopy);
    cloneUserSettingsFromClonedCompanyIntoOkrBranchForPreparation(
        okrCompanyToClone, okrCompanyCopy);
  }

  private void cloneChildUnitListIntoParentUnitForPreparation(
      Collection<OkrChildUnit> okrChildUnitListToClone, OkrUnit okrUnitToCloneInto) {
    okrChildUnitListToClone.forEach(
        original -> cloneChildUnitIntoParentUnitForPreparation(original, okrUnitToCloneInto));
  }

  private void cloneChildUnitIntoParentUnitForPreparation(
      OkrChildUnit okrChildUnitToClone, OkrUnit okrUnitToCloneInto) {
    OkrChildUnit copy = okrChildUnitToClone.getCopyWithoutRelations();

    if (okrUnitToCloneInto instanceof OkrParentUnit) {
      copy.setParentOkrUnit(okrUnitToCloneInto);
      ((OkrParentUnit) okrUnitToCloneInto).getOkrChildUnits().add(copy);
    }

    subUnitRepository.save(copy);
    cloneObjectiveListIntoOkrUnitForPreparation(okrChildUnitToClone.getObjectives(), copy);

    if (okrChildUnitToClone instanceof OkrParentUnit) {
      cloneChildUnitListIntoParentUnitForPreparation(
          ((OkrParentUnit) okrChildUnitToClone).getOkrChildUnits(), copy);
    }
  }

  private void cloneObjectiveListIntoOkrUnitForPreparation(
      Collection<Objective> objectiveListToClone, OkrUnit okrUnitToCloneInto) {
    for (Objective original : objectiveListToClone) {
      cloneObjectiveIntoOkrUnitForPreparation(original, okrUnitToCloneInto);
    }
  }

  private void cloneObjectiveIntoOkrUnitForPreparation(
      Objective objectiveToClone, OkrUnit okrUnitToCloneInto) {
    Objective copy = objectiveToClone.getCopyWithoutRelations();
    copy.setParentOkrUnit(okrUnitToCloneInto);
    okrUnitToCloneInto.getObjectives().add(copy);
    if (objectiveToClone.hasParentObjective()) {
      Objective clonedParentObjective = clonedObjectives.get(objectiveToClone.getParentObjective());
      if (clonedParentObjective != null) {
        copy.setParentObjective(clonedParentObjective);
      }
    }
    clonedObjectives.put(objectiveToClone, copy);
    objectiveRepository.save(copy);
  }

  private void cloneUserSettingsFromClonedCompanyIntoOkrBranchForPreparation(
      OkrCompany okrCompanyToClone, OkrCompany okrCompanyCopy) {
    Iterable<UserSettings> userSettingsIter = userSettingsRepository.findAll();
    for (UserSettings userSettings : userSettingsIter) {
      if (userSettings.getDefaultOkrCompany() != null
          && userSettings.getDefaultOkrCompany().equals(okrCompanyToClone)) {
        userSettings.setDefaultOkrCompany(okrCompanyCopy);
        if (userSettings.getDefaultTeam() != null) {
          OkrDepartment newTeamCopy =
              findNewTeamCopy(userSettings.getDefaultTeam(), okrCompanyCopy);
          userSettings.setDefaultTeam(newTeamCopy);
        }
        userSettingsRepository.save(userSettings);
      }
    }
  }

  private OkrDepartment findNewTeamCopy(OkrDepartment oldTeam, OkrCompany okrCompany) {
    Collection<OkrDepartment> okrDepartments = BranchHelper.collectDepartments(okrCompany);

    for (OkrDepartment okrDepartment : okrDepartments) {
      if (okrDepartment.getName().equals(oldTeam.getName())) {
        return okrDepartment;
      }
    }

    return null;
  }
}