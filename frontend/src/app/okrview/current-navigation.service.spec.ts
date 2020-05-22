import { TestBed } from '@angular/core/testing';

import { CurrentNavigationService } from './current-navigation.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA } from '@angular/core';
import { CurrentDepartmentStructureService } from './current-department-structure.service';
import { CurrentDepartmentStructureServiceMock } from '../shared/mocks/current-department-structure-service-mock';

describe('CurrentNavigationService', () => {
  const currentDepartmentStructureServiceMock: CurrentDepartmentStructureServiceMock = new CurrentDepartmentStructureServiceMock();

  beforeEach(() => TestBed.configureTestingModule({
    declarations: [],
    imports: [
      HttpClientTestingModule,
    ],
    schemas: [ CUSTOM_ELEMENTS_SCHEMA, NO_ERRORS_SCHEMA ],
    providers: [
      { provide: CurrentDepartmentStructureService, useValue: currentDepartmentStructureServiceMock }
    ]
  }));

  it('should be created', () => {
    const service: CurrentNavigationService = TestBed.get(CurrentNavigationService);
    expect(service)
      .toBeTruthy();
  });
});
