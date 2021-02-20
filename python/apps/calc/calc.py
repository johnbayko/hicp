from enum import Enum

from hicp import HICP, EventType, Message, Panel, Window, Label, Button, TextField
from hicp import App, AppInfo

class CalcOp(Enum):
    EQ = (1, '=')
    ADD = (2, '+')
    SUB = (2, '-')
    MUL = (3, '*')
    DIV = (3, '/')
    def __init__(self, precedence, text):
        self.precedence = precedence
        self.text = text

class CalcData:
    def __init__(self):
        self.memory_value = 0.0
        self.reset()

    def reset(self):
        self.current_value = 0.0

        self.value_stack = []
        self.op_stack = [CalcOp.EQ] # EQ placeholder avoids checking len() == 0

        # Keep track of external display state
        # TODO Maybe should subclass TextField and put this in there?
        self.new_value = True

    def perform_op(self, value_string, op):
        try:
            value = float(value_string)
        except ValueError:
            # Cannot apply value_string to current value, so skip.
            return
        self.value_stack.append(value)

        # Evaluate any ops on stack
        while self.op_stack[-1].precedence >= op.precedence:
            if CalcOp.EQ == self.op_stack[-1]:
                break

            stack_op = self.op_stack.pop()
            # Remember stack reverses things, left was pushed earlier so
            # comes off after right.
            right_value = self.value_stack.pop()
            left_value = self.value_stack.pop()

            if CalcOp.ADD == stack_op:
                new_value = left_value + right_value
            elif CalcOp.SUB == stack_op:
                new_value = left_value - right_value
            elif CalcOp.MUL == stack_op:
                new_value = left_value * right_value
            elif CalcOp.DIV == stack_op:
                new_value = left_value / right_value

            self.value_stack.append(new_value)

        if self.op_stack[-1].precedence < op.precedence:
            self.op_stack.append(op)

        # For display
        self.current_value = self.value_stack[-1]

class DigitClickHandler:
    def __init__(self, calc_data, digit, display_field):
        self.calc_data = calc_data
        self.digit = digit
        self.display_field = display_field

    def update(self, hicp, event_message, digit_button):   
        content = self.display_field.get_content()
        if self.calc_data.new_value:
            # displayed value gets replaced with digit being entered.
            content = ''
            self.calc_data.new_value = False
        content += self.digit
        self.display_field.set_content(content)
        self.display_field.update()

class DecimalClickHandler:
    def __init__(self, digit, display_field):
        self.digit = digit
        self.display_field = display_field

    def update(self, hicp, event_message, digit_button):   
        content = self.display_field.get_content()
        if self.digit in content:
            # Only one decimal allowed
            return
        content += self.digit
        self.display_field.set_content(content)
        self.display_field.update()

class SignClickHandler:
    def __init__(self, display_field):
        self.display_field = display_field

    def update(self, hicp, event_message, digit_button):   
        content = self.display_field.get_content()
        if '0' == content:
            # No negative 0
            return
        if '-' == content[0]:
            content = content[1:]
        else:
            content = '-' + content
        self.display_field.set_content(content)
        self.display_field.update()

class OpClickHandler:
    def __init__(self, op, display_field, calc_data):
        self.op = op
        self.display_field = display_field
        self.calc_data = calc_data

    def update(self, hicp, event_message, op_button):
        content = self.display_field.get_content()

        self.calc_data.perform_op(content, self.op)

        self.display_field.set_content(str(self.calc_data.current_value))
        self.calc_data.new_value = True

        self.display_field.update()

class MemStoClickHandler:
    def __init__(self, display_field, calc_data):
        self.display_field = display_field
        self.calc_data = calc_data

    def update(self, hicp, event_message, op_button):
        try:
            value = float(self.display_field.get_content())
            self.calc_data.memory_value = value
        except ValueError:
            # No valid value, nothing to do
            pass

class MemRclClickHandler:
    def __init__(self, display_field, calc_data):
        self.display_field = display_field
        self.calc_data = calc_data

    def update(self, hicp, event_message, op_button):
        self.display_field.set_content(str(self.calc_data.memory_value))
        self.display_field.update()

class ClrClickHandler:
    def __init__(self, display_field, calc_data):
        self.display_field = display_field
        self.calc_data = calc_data

    def update(self, hicp, event_message, op_button):
        self.calc_data.reset()

        self.display_field.set_content('0')
        self.display_field.update()

class Calc(App):
    @classmethod
    def get_app_name(cls):
        return 'calc'

    @classmethod
    def get_app_info(cls):
        app_name = cls.get_app_name()
        display_name = [('Calculator', 'en')]
        desc = [('Demo calculator.', 'en')]

        return AppInfo(app_name, display_name, desc)

    def connected(self, hicp):
        calc_data = CalcData()

        app_info = self.get_app_info()
        (text_group, text_subgroup) = hicp.get_text_group()

        window = self.new_app_window()
        window.set_text(app_info.display_name.get_text(text_group, text_subgroup), hicp)
        hicp.add(window)

        display_field = TextField()
        display_field.set_content('0') # Initial value
        window.add(display_field, 0, 0)

        digit = '7'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 0, 1)

        digit = '8'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 1, 1)

        digit = '9'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 2, 1)

        digit = '4'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 0, 2)

        digit = '5'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 1, 2)

        digit = '6'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 2, 2)

        digit = '1'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 0, 3)

        digit = '2'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 1, 3)

        digit = '3'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 2, 3)

        digit = '+/-'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, SignClickHandler(display_field))
        window.add(button, 0, 4)

        digit = '0'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DigitClickHandler(calc_data, digit, display_field))
        window.add(button, 1, 4)

        digit = '.'
        button = Button()
        button.set_text(digit, hicp)
        button.set_handler(EventType.CLICK, DecimalClickHandler(digit, display_field))
        window.add(button, 2, 4)

        # Buttons tend to go most common op at the bottom
        op = CalcOp.DIV
        button = Button()
        button.set_text(op.text, hicp)
        button.set_handler(EventType.CLICK, OpClickHandler(op, display_field, calc_data))
        window.add(button, 3, 1)

        op = CalcOp.MUL
        button = Button()
        button.set_text(op.text, hicp)
        button.set_handler(EventType.CLICK, OpClickHandler(op, display_field, calc_data))
        window.add(button, 3, 2)

        op = CalcOp.SUB
        button = Button()
        button.set_text(op.text, hicp)
        button.set_handler(EventType.CLICK, OpClickHandler(op, display_field, calc_data))
        window.add(button, 3, 3)

        op = CalcOp.ADD
        button = Button()
        button.set_text(op.text, hicp)
        button.set_handler(EventType.CLICK, OpClickHandler(op, display_field, calc_data))
        window.add(button, 3, 4)

        op = CalcOp.EQ
        button = Button()
        button.set_text(op.text, hicp)
        button.set_handler(EventType.CLICK, OpClickHandler(op, display_field, calc_data))
        window.add(button, 4, 4)

        button = Button()
        button.set_text('CLR', hicp)
        button.set_handler(EventType.CLICK, ClrClickHandler(display_field, calc_data))
        window.add(button, 4, 1)

        button = Button()
        button.set_text('M STO', hicp)
        button.set_handler(EventType.CLICK, MemStoClickHandler(display_field, calc_data))
        window.add(button, 4, 2)

        button = Button()
        button.set_text('M RCL', hicp)
        button.set_handler(EventType.CLICK, MemRclClickHandler(display_field, calc_data))
        window.add(button, 4, 3)

        window.set_visible(True)
        window.update()

