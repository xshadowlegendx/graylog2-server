/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import styled from 'styled-components';

import { Col, Panel, PanelGroup } from 'components/bootstrap';
import type { MigrationActions, StepArgs, MigrationStateItem, MigrationStepComponentProps } from 'components/datanode/Types';
import {
  IN_PLACE_MIGRATION_STEPS,
  MIGRATION_STATE,
} from 'components/datanode/Constants';
import Welcome from 'components/datanode/migrations/in-place/Welcome';
import CertificatesProvisioning from 'components/datanode/migrations/common/CertificatesProvisioning';
import JournalDowntimeWarning from 'components/datanode/migrations/in-place/JournalDowntimeWarning';
import StopMessageProcessing from 'components/datanode/migrations/in-place/StopMessageProcessing';
import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';
import RestartGraylog from 'components/datanode/migrations/in-place/RestartGraylog';
import MigrationError from 'components/datanode/migrations/common/MigrationError';

const StyledTitle = styled.h3`
  margin-bottom: 10px;

  & > small {
    font-size: 80%;
  }
`;

const StyledPanelGroup = styled(PanelGroup)`
  &.panel-group > .panel {
    margin-top: 0;
    border-color: ${(props) => props.theme.colors.input.border};
    background-color: ${(props) => props.theme.colors.global.contentBackground};

    .panel-heading {
      background-color: ${(props) => props.theme.colors.table.backgroundAlt};
    }

    &:not(:first-child) {
      border-top: 0;
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }

    &:not(:last-child) {
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
    }
  }
`;

const InPlaceMigration = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const { state: activeStep } = currentStep;

  const onStepComplete = async (step: MigrationActions, args: StepArgs = {}) => onTriggerStep(step, args);

  const getStepComponent = (step: MigrationStateItem) => {
    switch (step) {
      case MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE.key:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE2.key:
        return <CompatibilityCheckStep currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES.key:
      case MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key:
        return <CertificatesProvisioning currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key:
        return <JournalDowntimeWarning currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.MESSAGE_PROCESSING_STOP.key:
        return <StopMessageProcessing currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.RESTART_GRAYLOG.key:
        return <RestartGraylog currentStep={currentStep} onTriggerStep={onStepComplete} />;
      default:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} />;
    }
  };

  return (
    <Col>
      <StyledTitle>In-Place migration</StyledTitle>
      <p>Follow these steps to migrate your existing OpenSearch version 2.x or 1.3.x cluster to Data
        Nodes.
      </p>
      <StyledPanelGroup accordion id="first" activeKey={activeStep} onSelect={() => {}}>
        {IN_PLACE_MIGRATION_STEPS.map((inPlaceStep, index) => {
          const { description } = MIGRATION_STATE[inPlaceStep];

          return (
            <Panel eventKey={inPlaceStep} key={inPlaceStep} collapsible={false}>
              <Panel.Heading>
                <Panel.Title>
                  <Panel.Toggle tabIndex={index}>{`${index + 1}. ${description}`}</Panel.Toggle>
                </Panel.Title>
              </Panel.Heading>
              <Panel.Collapse>
                <Panel.Body>
                  <MigrationError errorMessage={currentStep.error_message} />
                  {getStepComponent(inPlaceStep)}
                </Panel.Body>
              </Panel.Collapse>
            </Panel>
          );
        })}

      </StyledPanelGroup>
    </Col>
  );
};

export default InPlaceMigration;
