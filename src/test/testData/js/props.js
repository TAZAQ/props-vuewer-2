export const baseProps = {
  id: {
    type: String,
    required: true
  },
  name: {
    type: String,
    default: 'Unnamed'
  },
  enabled: {
    type: Boolean,
    default: false
  }
};

export const advancedProps = {
  data: {
    type: Object,
    required: true
  },
  callback: {
    type: Function,
    validator: (value) => typeof value === 'function'
  }
};

export default {
    data: {
        type: Object,
        required: true
    },
    callback: {
        type: Function,
        validator: (value) => typeof value === 'function'
    }
}