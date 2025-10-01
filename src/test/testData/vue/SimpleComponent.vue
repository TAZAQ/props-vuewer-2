<template>
  <div>
    <h1>{{ message }}</h1>
    <p>Count: {{ count }}</p>
  </div>
</template>

<script>
import props, {advancedProps} from "../js/props";

export default {
  name: 'SimpleComponent',
  props: {
    message: {
      type: String,
      required: true
    },
    count: {
      type: Number,
      default: 0
    },

    ...advancedProps,

    ...props
  }
}
</script>
