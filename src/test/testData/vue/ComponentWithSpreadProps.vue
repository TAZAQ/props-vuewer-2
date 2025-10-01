<template>
  <div>
    <h1>{{ title }}</h1>
  </div>
</template>

<script>
const commonProps = {
  title: {
    type: String,
    required: true
  },
  visible: {
    type: Boolean,
    default: true
  }
};

export default {
  name: 'ComponentWithSpreadProps',
  props: {
    ...commonProps,
    count: Number
  }
}
</script>
