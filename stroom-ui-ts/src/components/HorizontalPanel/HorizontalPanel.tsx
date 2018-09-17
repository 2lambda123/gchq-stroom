import * as React from "react";
import { compose, withState, lifecycle } from "recompose";
import * as Mousetrap from "mousetrap";

import Button from "../Button";

const enhance = compose(
  withState("activeItem", "setActiveItem", "home"),
  lifecycle({
    componentDidMount() {
      const { onClose } = this.props;
      Mousetrap.bind("esc", () => onClose());
    },
    componentWillUnmount() {
      Mousetrap.unbind("esc");
    }
  })
);

const HorizontalPanel = ({
  title,
  headerMenuItems,
  content,
  activeItem,
  setActiveItem,
  onClose,
  headerSize
}) => (
  <div className="horizontal-panel">
    <div className="horizontal-panel__header flat">
      <div className="horizontal-panel__header__title">{title}</div>
      {headerMenuItems}
      <Button icon="times" onClick={() => onClose()} />
    </div>
    <div className="horizontal-panel__content">{content}</div>
  </div>
);

// HorizontalPanel.propTypes = {
//   content: PropTypes.object.isRequired,
//   title: PropTypes.object.isRequired,
//   headerMenuItems: PropTypes.array,
//   onClose: PropTypes.func.isRequired,
//   headerSize: PropTypes.string,
// };

export default enhance(HorizontalPanel);
